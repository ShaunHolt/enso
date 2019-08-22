package org.enso.syntax.graph

import org.enso.data.List1
import org.enso.syntax.graph.Extensions._
import org.enso.syntax.graph.CommonAPI.Module.Id
import org.enso.syntax.text.AST
import org.enso.syntax.graph.CommonAPI._
import org.enso.syntax.text.AST.Cons
import org.scalatest._

import scala.reflect.ClassTag

/** Mock project state - contains a single module named `Main` with given body.
  */
final case class StateManagerMock(var program: String) extends StateManager {
  var ast: AST.Module = AstUtils.parse(program)

  override def availableModules(): Seq[Module.Id] =
    Seq(StateManagerMock.mainModule)

  override def getModuleAst(module: Id): AST.Module = {
    val moduleAst = AstUtils.parse(program)
    var counter   = 0;
    moduleAst.map(ast => {
      // unmarked node asts should get their markers
      if (ast.requiresId) {
        val markedAst = AST.Marked(AST.Marker(counter), ast)
        counter += 1
        markedAst
      } else
        ast
    })
  }

  override def setModuleAst(module: Id, ast: AST.Module): Unit = {
    if (module != StateManagerMock.mainModule)
      throw new Exception(s"no such module: $module")

    this.program = ast.show()
    this.ast     = ast
    println(s"New AST for module $module: $ast")
    println(s"New Program text for module $module:\n$program")
  }
}

final case class NotificationConsumerMock() extends NotificationConsumer {
  var notificationsReceived: Seq[API.Notification] = Seq()
  override def send(notification: API.Notification): Unit = {
    println(s"Got notification: $notification")
    notificationsReceived = notificationsReceived :+ notification
  }
}

object StateManagerMock {
  val mainModule: Module.Id = List1(Cons("Main"))
}

class Tests extends FunSuite with org.scalatest.Matchers {
  val mockModule = StateManagerMock.mainModule

  def withDR[R](
    program: String,
    f: DoubleRepresentation => R
  ): (R, AST.Module) = {
    val state                = StateManagerMock(program)
    val notificationConsumer = NotificationConsumerMock()
    val result               = f(DoubleRepresentation(state, notificationConsumer))
    (result, state.ast)
  }

  def checkThatTransforms[R](
    initialProgram: String,
    expectedFinalProgram: String,
    action: DoubleRepresentation => R
  ): R = {
    val (result, finalAst) = withDR(initialProgram, action)
    val actualFinalProgram = finalAst.show()
    actualFinalProgram should be(expectedFinalProgram)
    result
  }
  def expectTransformationError[E: ClassTag](
    initialProgram: String,
    action: DoubleRepresentation => Unit
  ): Unit = {
    an[E] should be thrownBy { withDR(initialProgram, action) }
    ()
  }
  def checkModuleSingleNodeGraph[R](
    program: String,
    action: API.Node.Info => R
  ): R = {
    withDR(
      program,
      dr => {
        val graph = dr.getGraph(Module.Graph.Location(mockModule))
        graph.nodes should have size 1
        graph.links should have size 0
        action(graph.nodes.head)
      }
    )._1
  }

  test("adding first import") {
    checkThatTransforms(
      "",
      "import Foo.Baz",
      _.importModule(mockModule, Module.Name("Foo.Baz"))
    )
  }
  test("adding second import") {
    checkThatTransforms(
      "import Foo.Bar",
      "import Foo.Bar\nimport Foo.Baz",
      _.importModule(mockModule, Module.Name("Foo.Baz"))
    )
  }
  test("adding import when there is empty line and definition") {
    checkThatTransforms(
      """import Foo
        |
        |import Foo.Bar
        |
        |add x y = x + y""".stripMargin,
      """import Foo
        |
        |import Foo.Bar
        |import Foo.Baz
        |
        |add x y = x + y""".stripMargin,
      _.importModule(mockModule, Module.Name("Foo.Baz"))
    )
  }
  test("adding duplicated import") {
    expectTransformationError[API.ImportAlreadyExistsException](
      "import Foo.Bar",
      _.importModule(mockModule, Module.Name("Foo.Bar"))
    )
  }
  test("removing the only import") {
    checkThatTransforms(
      "import Foo.Baz",
      "",
      _.removeImport(mockModule, Module.Name("Foo.Baz"))
    )
  }
  test("removing one of several imports") {
    checkThatTransforms(
      """import Foo
        |import Foo.Baz
        |import Foo.Bar
        |add x y = x + y""".stripMargin,
      """import Foo
        |import Foo.Bar
        |add x y = x + y""".stripMargin,
      _.removeImport(mockModule, Module.Name("Foo.Baz"))
    )
  }
  test("removing import between empty lines") {
    checkThatTransforms(
      """import Foo
        |
        |import Foo.Baz
        |
        |add x y = x + y""".stripMargin,
      """import Foo
        |
        |
        |add x y = x + y""".stripMargin,
      _.removeImport(mockModule, Module.Name("Foo.Baz"))
    )
  }
  test("get empty module graph") {
    withDR(
      "",
      dr => {
        val graph = dr.getGraph(Module.Graph.Location(mockModule))
        graph.nodes should have size 0
        graph.links should have size 0
      }
    )
  }
  test("no nodes from function def") {
    withDR(
      "add a b = a + b",
      dr => {
        val graph = dr.getGraph(Module.Graph.Location(mockModule))
        graph.nodes should have size 0
        graph.links should have size 0
      }
    )
  }
  test("get trivial literal node") {
    checkModuleSingleNodeGraph(
      "15",
      node => {
        node.expr.text should equal("15")
        node.inputs should have size 0
        node.output.name should equal(None)
        node.flags should equal(Set.empty)
      }
    )
  }
  test("get trivial var node") {
    checkModuleSingleNodeGraph(
      "foo",
      node => {
        node.expr.text should equal("foo")
        node.inputs should have size 0
        node.output.name should equal(None)
        node.flags should equal(Set.empty)
      }
    )
  }
  test("get trivial named node") {
    checkModuleSingleNodeGraph(
      "a = 15",
      node => {
        node.expr.text should equal("15")
        node.inputs should have size 0
        node.output.name should equal(Some("a"))
        node.flags should equal(Set.empty)
      }
    )
  }
}
