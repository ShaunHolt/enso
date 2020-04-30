package org.enso.interpreter.node.expression.builtin.java;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.enso.interpreter.Language;
import org.enso.interpreter.node.expression.builtin.BuiltinRootNode;
import org.enso.interpreter.runtime.Context;
import org.enso.interpreter.runtime.callable.argument.ArgumentDefinition;
import org.enso.interpreter.runtime.callable.function.Function;
import org.enso.interpreter.runtime.callable.function.FunctionSchema.CallStrategy;
import org.enso.interpreter.runtime.interop.JavaObject;
import org.enso.interpreter.runtime.state.Stateful;

/** An implementation of generic JSON serialization. */
@NodeInfo(shortName = "Java.add_to_classpath", description = "Generic JSON serialization.")
public class Execute2Node extends BuiltinRootNode {
  private Execute2Node(Language language) {
    super(language);
  }

  private @Child InteropLibrary library = InteropLibrary.getFactory().createDispatched(10);
  private final BranchProfile err = BranchProfile.create();

  /**
   * Creates a function wrapping this node.
   *
   * @param language the current language instance
   * @return a function wrapping this node
   */
  public static Function makeFunction(Language language) {
    return Function.fromBuiltinRootNode(
        new Execute2Node(language),
        CallStrategy.ALWAYS_DIRECT,
        new ArgumentDefinition(0, "this", ArgumentDefinition.ExecutionMode.EXECUTE),
        new ArgumentDefinition(1, "obj", ArgumentDefinition.ExecutionMode.EXECUTE),
        new ArgumentDefinition(2, "arg1", ArgumentDefinition.ExecutionMode.EXECUTE),
        new ArgumentDefinition(3, "arg2", ArgumentDefinition.ExecutionMode.EXECUTE));
  }

  /**
   * Executes the node.
   *
   * @param frame current execution frame.
   * @return the result of converting input into a string.
   */
  @Override
  public Stateful execute(VirtualFrame frame) {
    Object[] args = Function.ArgumentsHelper.getPositionalArguments(frame.getArguments());
    Object obj = args[1];
    Object arg1 = args[2];
    Object arg2 = args[3];
    Object state = Function.ArgumentsHelper.getState(frame.getArguments());
    try {
      Object res = library.execute(obj, arg1, arg2);
      return new Stateful(state, res);
    } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
      err.enter();
      return null;
    }
  }

  /**
   * Returns a language-specific name for this node.
   *
   * @return the name of this node
   */
  @Override
  public String getName() {
    return "Java.execute2";
  }
}
