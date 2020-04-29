package org.enso.interpreter.test.semantic

import org.enso.interpreter.test.InterpreterTest

class JavaInteropTest extends InterpreterTest {

  val code =
    """
      |main =
      |    Java.add_to_class_path "/Users/marcinkostrzewa/code/javatests/"
      |    cls = Java.lookup_class "org.enso.xD.Tester"
      |
      |    cls
      |""".stripMargin

  println(eval(code))
}
