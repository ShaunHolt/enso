package org.enso.interpreter.node.expression.builtin.java;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
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
public class GetMemberNode extends BuiltinRootNode {
  private GetMemberNode(Language language) {
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
        new GetMemberNode(language),
        CallStrategy.ALWAYS_DIRECT,
        new ArgumentDefinition(0, "this", ArgumentDefinition.ExecutionMode.EXECUTE),
        new ArgumentDefinition(1, "obj", ArgumentDefinition.ExecutionMode.EXECUTE),
        new ArgumentDefinition(2, "name", ArgumentDefinition.ExecutionMode.EXECUTE));
  }

  /**
   * Executes the node.
   *
   * @param frame current execution frame.
   * @return the result of converting input into a string.
   */
  @Override
  public Stateful execute(VirtualFrame frame) {
    Object obj = Function.ArgumentsHelper.getPositionalArguments(frame.getArguments())[1];
    String name = (String) Function.ArgumentsHelper.getPositionalArguments(frame.getArguments())[2];
    Object state = Function.ArgumentsHelper.getState(frame.getArguments());
    try {
      Object res = library.readMember(obj, name);
      return new Stateful(state, res);
    } catch (UnsupportedMessageException | UnknownIdentifierException e) {
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
    return "Java.get_member";
  }
}
