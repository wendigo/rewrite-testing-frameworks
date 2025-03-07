/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.assertj;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

public class JUnitAssertTrueToAssertThat extends Recipe {
    @Override
    public String getDisplayName() {
        return "JUnit `assertTrue` to AssertJ";
    }

    @Override
    public String getDescription() {
        return "Convert JUnit-style `assertTrue()` to AssertJ's `assertThat().isTrue()`.";
    }

  @Override
  public Duration getEstimatedEffortPerOccurrence() {
    return Duration.ofMinutes(5);
  }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("org.junit.jupiter.api.Assertions", false);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AssertTrueToAssertThatVisitor();
    }

    public static class AssertTrueToAssertThatVisitor extends JavaIsoVisitor<ExecutionContext> {
        private Supplier<JavaParser> assertionsParser;
        private Supplier<JavaParser> assertionsParser(ExecutionContext ctx) {
            if(assertionsParser == null) {
                assertionsParser = () -> JavaParser.fromJavaVersion()
                        .classpathFromResources(ctx, "assertj-core-3.24.2")
                        .build();
            }
            return assertionsParser;
        }
        private static final MethodMatcher JUNIT_ASSERT_TRUE = new MethodMatcher("org.junit.jupiter.api.Assertions" + " assertTrue(boolean, ..)");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (!JUNIT_ASSERT_TRUE.matches(method)) {
                return method;
            }

            List<Expression> args = method.getArguments();
            Expression actual = args.get(0);

            if (args.size() == 1) {
                method = method.withTemplate(
                        JavaTemplate.builder(this::getCursor, "assertThat(#{any(boolean)}).isTrue();")
                                .staticImports("org.assertj.core.api.Assertions.assertThat")
                                .javaParser(assertionsParser(ctx))
                                .build(),
                        method.getCoordinates().replace(),
                        actual
                );
            } else {
                Expression message = args.get(1);

                JavaTemplate.Builder template = TypeUtils.isString(message.getType()) ?
                        JavaTemplate.builder(this::getCursor, "assertThat(#{any(boolean)}).as(#{any(String)}).isTrue();") :
                        JavaTemplate.builder(this::getCursor, "assertThat(#{any(boolean)}).as(#{any(java.util.function.Supplier)}).isTrue();");

                method = method.withTemplate(template
                                .staticImports("org.assertj.core.api.Assertions.assertThat")
                                .javaParser(assertionsParser(ctx))
                                .build(),
                        method.getCoordinates().replace(),
                        actual,
                        message
                );
            }

            maybeAddImport("org.assertj.core.api.Assertions", "assertThat");
            maybeRemoveImport("org.junit.jupiter.api.Assertions");

            return method;
        }
    }
}
