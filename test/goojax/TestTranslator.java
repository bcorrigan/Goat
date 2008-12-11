package goojax;

import junit.framework.*;
import static goojax.GooJAXFetcher.Language;

public class TestTranslator extends TestCase { 
public void testTranslator() {
		try {
			String query = "Yo soy un perro hablando";
			Translator tranny = new Translator();
			DetectLanguageResponse dlrs = tranny.detect(query);
			DetectLanguageResponseData dlrd = dlrs.getResponseData();
			System.out.println("Language Detection test:");
			assertTrue(dlrd.getLanguage().equals(Language.ES));
			assertTrue(dlrd.isReliable());
			System.out.println(		
					"   query: " + tranny.getQuery() + 
					"\n   Language: " + dlrd.getLanguage().englishName +
					"\n   Reliable? " + (dlrd.isReliable ? "yes":"no") +
					"\n   Confidence: " + dlrd.getConfidence());
			TranslateResponse tr = tranny.translate();
			TranslateResponseData trd = tr.getResponseData();
			assertTrue(null != trd);
			System.out.println("Translation test:");
			System.out.println(		
					"   query: " + tranny.getQuery() +
					"\n   Detected Language: " + trd.getDetectedSourceLanguage() +
					"\n   Translation: " + trd.getTranslatedText());
			trd = tranny.translate("Je suis un chat du mer", Language.FR, Language.ES).getResponseData();
			System.out.println(		
					"   query: " + tranny.getQuery() +
					"\n   Detected Language: " + trd.getDetectedSourceLanguage() +
					"\n   Translation: " + trd.getTranslatedText());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
}
