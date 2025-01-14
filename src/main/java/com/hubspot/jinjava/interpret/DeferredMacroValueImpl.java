package com.hubspot.jinjava.interpret;

public class DeferredMacroValueImpl implements DeferredValue {
  private static final DeferredValue INSTANCE = new DeferredMacroValueImpl();

  private DeferredMacroValueImpl() {}

  @Override
  public Object getOriginalValue() {
    return null;
  }

  public static DeferredValue instance() {
    return INSTANCE;
  }
}
