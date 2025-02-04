/**********************************************************************
 * Copyright (c) 2014 HubSpot Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **********************************************************************/
package com.hubspot.jinjava.lib.tag;

import com.google.common.collect.ImmutableMap;
import com.hubspot.jinjava.doc.annotations.JinjavaDoc;
import com.hubspot.jinjava.doc.annotations.JinjavaParam;
import com.hubspot.jinjava.doc.annotations.JinjavaSnippet;
import com.hubspot.jinjava.interpret.IncludeTagCycleException;
import com.hubspot.jinjava.interpret.InterpretException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateError;
import com.hubspot.jinjava.interpret.TemplateError.ErrorItem;
import com.hubspot.jinjava.interpret.TemplateError.ErrorReason;
import com.hubspot.jinjava.interpret.TemplateError.ErrorType;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import com.hubspot.jinjava.interpret.errorcategory.BasicTemplateErrorCategory;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.TagNode;
import com.hubspot.jinjava.util.HelperStringTokenizer;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@JinjavaDoc(
  value = "includes multiple files in one template or stylesheet",
  params = {
    @JinjavaParam(
      value = "path",
      desc = "Design Manager path to the file that you would like to include"
    )
  },
  snippets = {
    @JinjavaSnippet(code = "{% include \"custom/page/web_page_basic/my_footer.html\" %}"),
    @JinjavaSnippet(code = "{% include \"generated_global_groups/2781996615.html\" %}"),
    @JinjavaSnippet(code = "{% include \"hubspot/styles/patches/recommended.css\" %}")
  }
)
public class IncludeTag implements Tag {
  public static final String TAG_NAME = "include";

  private static final long serialVersionUID = -8391753639874726854L;

  @Override
  public String interpret(TagNode tagNode, JinjavaInterpreter interpreter) {
    HelperStringTokenizer helper = new HelperStringTokenizer(tagNode.getHelpers());
    if (!helper.hasNext()) {
      throw new TemplateSyntaxException(
        tagNode.getMaster().getImage(),
        "Tag 'include' expects template path",
        tagNode.getLineNumber(),
        tagNode.getStartPosition()
      );
    }

    String path = StringUtils.trimToEmpty(helper.next());
    String templateFile = interpreter.resolveString(
      path,
      tagNode.getLineNumber(),
      tagNode.getStartPosition()
    );
    templateFile = interpreter.resolveResourceLocation(templateFile);

    /* check next tokens if it could be ignored if missing */
    boolean ignoreMissing = checkIgnoreMissing(helper);

    try {
      interpreter
        .getContext()
        .getIncludePathStack()
        .push(templateFile, tagNode.getLineNumber(), tagNode.getStartPosition());
    } catch (IncludeTagCycleException e) {
      interpreter.addError(
        new TemplateError(
          ErrorType.WARNING,
          ErrorReason.EXCEPTION,
          ErrorItem.TAG,
          "Include cycle detected for path: '" + templateFile + "'",
          null,
          tagNode.getLineNumber(),
          tagNode.getStartPosition(),
          e,
          BasicTemplateErrorCategory.INCLUDE_CYCLE_DETECTED,
          ImmutableMap.of("path", templateFile)
        )
      );
      return "";
    }

    try {
      String template = interpreter.getResource(templateFile);
      Node node = interpreter.parse(template);

      interpreter.getContext().addDependency("coded_files", templateFile);
      interpreter
        .getContext()
        .getCurrentPathStack()
        .push(templateFile, interpreter.getLineNumber(), interpreter.getPosition());

      return interpreter.render(node, false);
    } catch (IOException e) {
      if (ignoreMissing) {
        return "";
      }

      throw new InterpretException(
        e.getMessage(),
        e,
        tagNode.getLineNumber(),
        tagNode.getStartPosition()
      );
    } finally {
      interpreter.getContext().getIncludePathStack().pop();
      interpreter.getContext().getCurrentPathStack().pop();
    }
  }

  /**
   * Returns true if the last two tokens match "ignore" "missing".
   *
   * @param helper
   */
  private boolean checkIgnoreMissing(HelperStringTokenizer helper) {
    List<String> all = helper.allTokens();
    if (
      all.size() >= 2 &&
      "ignore".equals(all.get(all.size() - 2)) &&
      "missing".equals(all.get(all.size() - 1))
    ) {
      return true;
    }
    return false;
  }

  @Override
  public String getEndTagName() {
    return null;
  }

  @Override
  public String getName() {
    return TAG_NAME;
  }
}
