/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.customtabs.trusted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.support.customtabs.trusted.sharing.ShareTarget;
import android.support.customtabs.trusted.sharing.ShareTarget.FileFormField;
import android.support.customtabs.trusted.sharing.ShareTarget.Params;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link SharingUtils#parseShareTargetJson}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class ShareTargetParserTest {

    private static final TestParams[] sTestParams = new TestParams[] {
            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"GET\", "
                    + "\"params\": {\"title\": \"received_title\", \"text\": \"received_text\"}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "GET", null /* encodingType */,
                            new Params("received_title", "received_text", null)
                    ),

                    "DefaultGet"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"GET\", "
                    + "\"params\": {\"text\": \"received_text\"}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "GET", null /* encodingType */,
                            new Params("title", "received_text", null)
                    ),

                    "NoTitle"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"GET\", "
                    + "\"params\": {\"title\": \"received_title\"}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "GET", null /* encodingType */,
                            new Params("received_title", "text", null)
                    ),

                    "NoText"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"POST\", "
                    + "\"params\": {\"title\": \"received_title\", \"text\": \"received_text\","
                    + "\"files\": [{\"name\": \"text_files\", \"accept\": \"text/*\" }, "
                    + "{\"name\": \"image_files\", \"accept\": \"image/*\"}]}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "POST", null /* encodingType */,
                            new Params("received_title",  "received_text", Arrays.asList(
                                        new FileFormField("text_files", Arrays.asList("text/*")),
                                        new FileFormField("image_files", Arrays.asList("image/*"))
                            ))
                    ),

                    "PostOneAcceptPerFile"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"POST\", "
                    + "\"params\": {\"title\": \"received_title\", \"text\": \"received_text\","
                    + "\"files\": [{\"name\": \"text_files\", \"accept\": [\"text/*\"]}, "
                    + "{\"name\": \"image_files\", \"accept\": [\"image/*\"]}]}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "POST", null /* encodingType */,
                            new Params("received_title",  "received_text", Arrays.asList(
                                    new FileFormField("text_files", Arrays.asList("text/*")),
                                    new FileFormField("image_files", Arrays.asList("image/*"))
                            ))
                    ),

                    "PostOneAcceptPerFileInBrackets"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"POST\", "
                    + "\"params\": {\"title\": \"received_title\", \"text\": \"received_text\","
                    + "\"files\": [{\"name\": \"text_files\", "
                    + "\"accept\": [\"text/html\", \"text/csv\"]}, "
                    + "{\"name\": \"image_files\", "
                    + "\"accept\": [\"image/png\", \"image/jpeg\", \"image/svg\"]}]}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "POST", null /* encodingType */,
                            new Params("received_title",  "received_text", Arrays.asList(
                                    new FileFormField("text_files",
                                            Arrays.asList("text/html", "text/csv")),
                                    new FileFormField("image_files",
                                            Arrays.asList("image/png", "image/jpeg", "image/svg"))
                            ))
                    ),

                    "PostMultipleAcceptPerFile"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"POST\", "
                    + "\"params\": {\"title\": \"received_title\", \"text\": \"received_text\"}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "POST", null /* encodingType */,
                            new Params("received_title",  "received_text", null)
                    ),
                    "PostNoFiles"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"POST\", "
                    + "\"enctype\": \"multipart/form-data\","
                    + "\"params\": {\"title\": \"received_title\", \"text\": \"received_text\","
                    + "\"files\": [{\"name\": \"text_files\", \"accept\": \"text/*\" }, "
                    + "{\"name\": \"image_files\", \"accept\": \"image/*\"}]}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "POST",  "multipart/form-data",
                            new Params("received_title",  "received_text", Arrays.asList(
                                    new FileFormField("text_files", Arrays.asList("text/*")),
                                    new FileFormField("image_files", Arrays.asList("image/*"))
                            ))
                    ),

                    "PostEnctypeMultipart"
            )
    };

    @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
    public static Collection<Object[]> parameters() {
        // Parameterized runner doesn't allow arbitrary objects to be passed as parameters,
        // so we can't pass ShareTarget objects. Instead we pass an index in sTestParams.
        // The test name is passed only to be used in the test name template
        // (see name = "{1}" above).
        List<Object[]> parameters = new ArrayList<>(sTestParams.length);
        for (int i = 0; i < sTestParams.length; i++) {
            parameters.add(new Object[] {i, sTestParams[i].testName});
        }
        return parameters;
    }


    private final String mInput;
    private final ShareTarget mExpectedOutput;

    public ShareTargetParserTest(int testIndex, String testName) {
        TestParams params = sTestParams[testIndex];
        mInput = params.input;
        mExpectedOutput = params.expectedOutput;
    }

    @Test
    public void parsingIsCorrect() throws Exception {
        assertShareTargetEquals(mExpectedOutput, SharingUtils.parseShareTargetJson(mInput));
    }

    private void assertShareTargetEquals(ShareTarget expected, ShareTarget actual) {
        assertEquals(expected.action, actual.action);
        assertEquals(expected.encodingType, actual.encodingType);
        assertEquals(expected.method, actual.method);
        assertParamsEqual(expected.params, actual.params);
    }

    private void assertParamsEqual(Params expected, Params actual) {
        assertEquals(expected.text, actual.text);
        assertEquals(expected.title, actual.title);
        assertFilesEqual(expected.files, actual.files);
    }

    private void assertFilesEqual(List<FileFormField> expected,
            List<FileFormField> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertFileEquals(expected.get(i), actual.get(i));
        }
    }

    private void assertFileEquals(FileFormField expected,
            FileFormField actual) {
        assertEquals(expected.name, actual.name);
        assertEquals(expected.acceptedTypes, actual.acceptedTypes);
    }

    private static class TestParams {
        public final String input;
        public final ShareTarget expectedOutput;
        public final String testName;

        private TestParams(String input, ShareTarget expectedOutput, String testName) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.testName = testName;
        }
    }
}
