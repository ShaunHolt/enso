package org.enso.interpreter.test.semantic

import org.enso.interpreter.test.InterpreterTest

class JavaInteropTest extends InterpreterTest {
  "java" should "java" in {
    val code =
      """
        |main =
        |    Java.add_to_class_path "/Users/marcinkostrzewa/code/javatests/"
        |    tester = Java.lookup_class "org.enso.xD.Tester"
        |    fun = Java.get_member tester "addNumbers"
        |    Java.execute2 fun 1 2
        |""".stripMargin

    eval(code) shouldEqual 3
  }
}
