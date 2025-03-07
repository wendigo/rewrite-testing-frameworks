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
package org.openrewrite.java.testing.mockito;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MockUtilsToStaticTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "mockito-all-1.10.19"))
          .recipe(new MockUtilsToStatic());
    }

    @DocumentExample
    @Test
    void basicInstanceToStaticSwap() {
        //language=java
        rewriteRun(
          java(
            """
              package mockito.example;

              import org.mockito.internal.util.MockUtil;
              
              public class MockitoMockUtils {
                  public void isMockExample() {
                      new MockUtil().isMock("I am a real String");
                  }
              }
              """,
            """
              package mockito.example;

              import org.mockito.internal.util.MockUtil;
              
              public class MockitoMockUtils {
                  public void isMockExample() {
                      MockUtil.isMock("I am a real String");
                  }
              }
              """
          )
        );
    }

    @Test
    void mockUtilsVariableToStatic() {
        //language=java
        rewriteRun(
          java(
            """
              package mockito.example;

              import org.mockito.internal.util.MockUtil;
              
              public class MockitoMockUtils {
                  public void isMockExample() {
                      MockUtil util = new MockUtil();
                      util.isMock("I am a real String");
                  }
              }
              """,
            """
              package mockito.example;

              import org.mockito.internal.util.MockUtil;

              public class MockitoMockUtils {
                  public void isMockExample() {
                      MockUtil.isMock("I am a real String");
                  }
              }
              """
          )
        );
    }

    @Test
    void mockUtilsFieldToStatic() {
        //language=java
        rewriteRun(
          java(
            """
              package mockito.example;

              import org.mockito.internal.util.MockUtil;
              
              public class MockitoMockUtils {
                  MockUtil util = new MockUtil();
                  public void isMockExample() {
                      util.isMock("I am a real String");
                  }
              }
              """,
            """
              package mockito.example;

              import org.mockito.internal.util.MockUtil;

              public class MockitoMockUtils {
                  public void isMockExample() {
                      MockUtil.isMock("I am a real String");
                  }
              }
              """
          )
        );
    }
}
