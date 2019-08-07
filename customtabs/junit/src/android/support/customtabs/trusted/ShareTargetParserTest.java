package android.support.customtabs.trusted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.support.customtabs.trusted.sharing.ShareTarget;

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
                            new ShareTarget.Params("received_title", "received_text", null)
                    ),

                    "DefaultGet"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"GET\", "
                    + "\"params\": {\"text\": \"received_text\"}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "GET", null /* encodingType */,
                            new ShareTarget.Params("title", "received_text", null)
                    ),

                    "NoTitle"
            ),

            new TestParams(
                    "{\"action\": \"https://pwa.rocks/share.html\", \"method\": \"GET\", "
                    + "\"params\": {\"title\": \"received_title\"}}",

                    new ShareTarget(
                            "https://pwa.rocks/share.html",
                            "GET", null /* encodingType */,
                            new ShareTarget.Params("received_title", "text", null)
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
                            new ShareTarget.Params("received_title",  "received_text", Arrays.asList(
                                    new ShareTarget.FileFormField("text_files",
                                            Arrays.asList("text/*")),
                                    new ShareTarget.FileFormField("image_files",
                                            Arrays.asList("image/*"))
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
                            new ShareTarget.Params("received_title",  "received_text", Arrays.asList(
                                    new ShareTarget.FileFormField("text_files",
                                            Arrays.asList("text/*")),
                                    new ShareTarget.FileFormField("image_files",
                                            Arrays.asList("image/*"))
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
                            new ShareTarget.Params("received_title",  "received_text", Arrays.asList(
                                    new ShareTarget.FileFormField("text_files",
                                            Arrays.asList("text/html", "text/csv")),
                                    new ShareTarget.FileFormField("image_files",
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
                            new ShareTarget.Params("received_title",  "received_text", null)
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
                            new ShareTarget.Params("received_title",  "received_text", Arrays.asList(
                                    new ShareTarget.FileFormField("text_files",
                                            Arrays.asList("text/*")),
                                    new ShareTarget.FileFormField("image_files",
                                            Arrays.asList("image/*"))
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

    private void assertParamsEqual(ShareTarget.Params expected, ShareTarget.Params actual) {
        assertEquals(expected.text, actual.text);
        assertEquals(expected.title, actual.title);
        assertFilesEqual(expected.files, actual.files);
    }

    private void assertFilesEqual(List<ShareTarget.FileFormField> expected,
            List<ShareTarget.FileFormField> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertFileEquals(expected.get(i), actual.get(i));
        }
    }

    private void assertFileEquals(ShareTarget.FileFormField expected,
            ShareTarget.FileFormField actual) {
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