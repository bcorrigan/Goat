package goat.module;

import static goat.util.StringUtil.scrub;
import static goat.util.TranslateWrapper.DEFAULT_GOAT_LANGUAGE;
import goat.core.Constants;
import goat.core.Module;
import goat.util.TranslateWrapper;

import java.util.Map;

import com.memetix.mst.language.Language;

public class Translation extends Module {

    public TranslateWrapper translator = new TranslateWrapper();

    @Override
    public String[] getCommands() {
        return new String[] { "translate", "languages", "detectlang", "scotchify" };
    }

    @Override
    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    @Override
    public void processChannelMessage(Message m) {
        String command = scrub(m.getModCommand()).toLowerCase();
        try {
            if ("translate".equals(command)) {
                ircTranslate(m);
            } else if ("detectlang".equals(command)) {
                ircDetectLanguage(m);
            } else if ("languages".equals(command)) {
                ircLanguages(m);
            } else if ("scotchify".equals(command)) {
                m.reply(goat.util.TextFilters.scotchify(m.getModTrailing()));
            }
        } catch (Exception e) {
            m.reply("Something went wrong:  " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ircTranslate(Message m) throws Exception {

        String text = scrub(m.getModTrailing());
        Language toLanguage = DEFAULT_GOAT_LANGUAGE;
        Language fromLanguage = null;

        int toFrom = 0;
        while (toFrom < 2
                && (text.toLowerCase().startsWith("to ") || text.toLowerCase()
                        .startsWith("from "))) {
            if (text.toLowerCase().startsWith("to ")) {
                if (text.length() < 4) {
                    m.reply("translate to...?");
                    return;
                }
                text = text.substring(3).trim();
                int spacepos = text.indexOf(' ');
                if (-1 == spacepos) {
                    m.reply("uh, I need at least two words after that \"to\" of yours");
                    return;
                }
                String langString = text.substring(0, spacepos).trim();
                text = text.substring(spacepos).trim();
                Language tempLang = langFromString(langString);
                if (null == tempLang) {
                    m.reply("Sorry, I don't speak \""
                            + langString
                            + "\".  Type \"languages\", and I'll tell you which ones I know.");
                    return;
                }
                toLanguage = tempLang;
                if (text.matches("\\s*")) {
                    m.reply("Er, what do you want me to translate to "
                            + toLanguage.getName(DEFAULT_GOAT_LANGUAGE));
                    return;
                }
            } else if (text.toLowerCase().startsWith("from ")) {
                if (text.length() < 6) {
                    m.reply("translate from...?");
                    return;
                }
                text = text.substring(5).trim();
                int spacepos = text.indexOf(' ');
                if (-1 == spacepos) {
                    m.reply("uh, I need at least two words after that \"from\" of yours");
                    return;
                }
                String langString = text.substring(0, spacepos).trim();
                text = text.substring(spacepos).trim();
                Language tempLang = langFromString(langString);
                if (null == tempLang) {
                    m.reply("Sorry, I don't speak \""
                            + langString
                            + "\".  Type \"languages\", and I'll tell you which ones I know.");
                    return;
                }
                fromLanguage = tempLang;
                if (text.matches("\\s*")) {
                    m.reply("Er, what do you want me to translate from "
                            + fromLanguage.getName(DEFAULT_GOAT_LANGUAGE));
                    return;
                }
            }
            toFrom++;
        }
        if (text.matches("\\s*")) {
            m.reply("Er, translate what, exactly?");
            return;
        }
        Boolean autoDetected = false;
        if (null == fromLanguage) {
            fromLanguage = translator.detect(text);
            // this is not what we want to do, ideally...
            if(! fromLanguage.equals(Language.ENGLISH)) // stupid API defaults to English if it can't detect the language
                autoDetected = true;
        }
        if (toLanguage.equals(fromLanguage))
            m.reply("I'm not going to translate that into the language it's already written in!");
        else if (autoDetected)
            m.reply("(from " + fromLanguage.getName(DEFAULT_GOAT_LANGUAGE)
                    + ")   " + translator.localize(m, translator.translate(text, toLanguage)));
        else
            m.reply(translator.localize(m, translator.translate(text, fromLanguage, toLanguage)));
    }

    private void ircDetectLanguage(Message m) throws Exception {
        if (scrub(m.getModTrailing()).matches("^\\s*$")) {
            m.reply("I detect a " + Constants.BOLD + "jerk" + Constants.NORMAL
                    + ", with a confidence of 1.0");
            return;
        }
        Language detectedLanguage = translator.detect(scrub(m.getModTrailing()));
        if (detectedLanguage != null && !detectedLanguage.toString().equals(""))
            m.reply("I think that's " + Constants.BOLD
                    + detectedLanguage.getName(DEFAULT_GOAT_LANGUAGE)
                    + Constants.NORMAL);
        else
            m.reply("I have no idea what kind of gibber-jabber that might be.");
    }

    private void ircLanguages(Message m) throws Exception {
        String msg = "I am fluent in:  ";

        Map<String,Language> localizedMap = Language.values(DEFAULT_GOAT_LANGUAGE);
        for(String langName : localizedMap.keySet()) {
            Language lang = localizedMap.get(langName);
            if (! lang.toString().equals(""))
            msg += lang.name().toLowerCase() + " (" + lang.toString() + "), ";
        }
        msg = msg.substring(0, msg.lastIndexOf(","));
        String tmp = msg.substring(msg.lastIndexOf(",") + 1);
        msg = msg.substring(0, msg.lastIndexOf(","));
        msg += " and" + tmp + ".";
        m.pagedReply(msg);
    }

    public Language langFromString(String str) throws Exception {
        str = scrub(str).toLowerCase();
        return translator.languageFromString(str);
    }

}
