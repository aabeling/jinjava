package com.hubspot.jinjava.lib.exptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import com.hubspot.jinjava.BaseJinjavaTest;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import com.hubspot.jinjava.objects.SafeString;
import org.junit.Test;

public class IsStringContainingExpTestTest extends BaseJinjavaTest {
  private static final String CONTAINING_TEMPLATE = "{{ var is string_containing arg }}";

  @Test
  public void itReturnsTrueForContainedString() {
    assertThat(
        jinjava.render(
          CONTAINING_TEMPLATE,
          ImmutableMap.of("var", "testing", "arg", "esti")
        )
      )
      .isEqualTo("true");
    assertThat(
        jinjava.render(CONTAINING_TEMPLATE, ImmutableMap.of("var", "testing", "arg", ""))
      )
      .isEqualTo("true");
    assertThat(
        jinjava.render(
          CONTAINING_TEMPLATE,
          ImmutableMap.of("var", "testing", "arg", "testing")
        )
      )
      .isEqualTo("true");
  }

  @Test
  public void itReturnsFalseForExcludedString() {
    assertThat(
        jinjava.render(
          CONTAINING_TEMPLATE,
          ImmutableMap.of("var", "testing", "arg", "blah")
        )
      )
      .isEqualTo("false");
  }

  @Test(expected = FatalTemplateErrorsException.class)
  public void itFailsForNull() {
    jinjava.render(CONTAINING_TEMPLATE, ImmutableMap.of("var", "testing"));
    fail("This line shouldn't be reached!");
  }

  @Test
  public void itWorksForSafeString() {
    assertThat(
        jinjava.render(
          CONTAINING_TEMPLATE,
          ImmutableMap.of("var", "testing", "arg", new SafeString("testing"))
        )
      )
      .isEqualTo("true");
  }
}
