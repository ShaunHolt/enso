package org.enso.interpreter.node.expression.builtin.java;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.enso.interpreter.Language;
import org.enso.interpreter.node.expression.builtin.BuiltinRootNode;
import org.enso.interpreter.runtime.Context;
import org.enso.interpreter.runtime.callable.argument.ArgumentDefinition;
import org.enso.interpreter.runtime.callable.function.Function;
import org.enso.interpreter.runtime.callable.function.FunctionSchema.CallStrategy;
import org.enso.interpreter.runtime.state.Stateful;

import java.io.File;

/** An implementation of generic JSON serialization. */
@NodeInfo(shortName = "Java.add_to_classpath", description = "Generic JSON serialization.")
public class AddToJavaClassPathNode extends BuiltinRootNode {
  private AddToJavaClassPathNode(Language language) {
    super(language);
  }

  /**
   * Creates a function wrapping this node.
   *
   * @param language the current language instance
   * @return a function wrapping this node
   */
  public static Function makeFunction(Language language) {
    return Function.fromBuiltinRootNode(
        new AddToJavaClassPathNode(language),
        CallStrategy.ALWAYS_DIRECT,
        new ArgumentDefinition(0, "this", ArgumentDefinition.ExecutionMode.EXECUTE),
        new ArgumentDefinition(1, "path", ArgumentDefinition.ExecutionMode.EXECUTE));
  }

  /**
   * Executes the node.
   *
   * @param frame current execution frame.
   * @return the result of converting input into a string.
   */
  @Override
  public Stateful execute(VirtualFrame frame) {
    Context ctx = lookupContextReference(Language.class).get();
    String arg = (String) Function.ArgumentsHelper.getPositionalArguments(frame.getArguments())[1];
    Object state = Function.ArgumentsHelper.getState(frame.getArguments());
    ctx.getEnvironment().addToHostClassPath(ctx.getTruffleFile(new File(arg)));
    return new Stateful(state, ctx.getBuiltins().unit());
  }

  /**
   * Returns a language-specific name for this node.
   *
   * @return the name of this node
   */
  @Override
  public String getName() {
    return "Java.append_to_host_classpath";
  }
}
