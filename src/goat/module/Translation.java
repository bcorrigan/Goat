package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.StringUtil;
import goat.util.Passwords;
import goat.util.TranslateWrapper;
import static goat.util.TranslateWrapper.DEFAULT_GOAT_LANGUAGE;

import java.util.Map;

import com.memetix.mst.language.Language;

public class Translation extends Module {

    public TranslateWrapper translator = new TranslateWrapper();
    
    public String[] getCommands() {
        return new String[] { "translate", "languages", "detectlang" };
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        String command = StringUtil
                .removeFormattingAndColors(m.getModCommand());
        try {
            if ("translate".equalsIgnoreCase(command)) {
                ircTranslate(m);
            } else if ("detectlang".equalsIgnoreCase(command)
                    || "langdetect".equalsIgnoreCase(command)
                    || "detectlanguage".equalsIgnoreCase(command)
                    || "languagedetect".equalsIgnoreCase(command)) {
                ircDetectLanguage(m);
            } else if ("languages".equalsIgnoreCase(command)) {
                ircLanguages(m);
            }
        } catch (Exception e) {
            m.reply("Something went wrong:  " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void ircTranslate(Message m) throws Exception {
        
        String text = StringUtil.removeFormattingAndColors(m.getModTrailing());
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
                Language tempLang = languageFromString(langString);
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
                Language tempLang = languageFromString(langString);
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
                    + ")   " + translator.translate(text, toLanguage));
        else
            m.reply(translator.translate(text, fromLanguage, toLanguage));
    }

    private void ircDetectLanguage(Message m) throws Exception {
        if (StringUtil.removeFormattingAndColors(m.getModTrailing()).matches("^\\s*$")) {
            m.reply("I detect a " + Constants.BOLD + "jerk" + Constants.NORMAL
                    + ", with a confidence of 1.0");
            return;
        }
        Language detectedLanguage = translator.detect(StringUtil
                .removeFormattingAndColors(m.getModTrailing()));
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
    
    private Language languageFromString(String str) throws Exception {
        str = StringUtil.removeFormattingAndColors(str.toLowerCase());
        Map<String, Language> langs = Language.values(DEFAULT_GOAT_LANGUAGE);
        Language ret = null;
        if (langs.containsKey(str))
            ret = langs.get(str);
        if (ret == null)
            for(Language l: langs.values())
                if (l.toString().equals(str)) {
                    ret = l;
                    break;
                }
        if (ret == null)
            for(Language l: langs.values())
                if (l.name().toLowerCase().equals(str.replaceAll("\\s", "_"))) {
                    ret = l;
                    break;
                }
        if (ret == null && str.length() > 3)
            for(Language l: langs.values())
                if (l.name().toLowerCase().startsWith(str)) {
                    ret = l;
                    break;
                }
        return ret;
    }

}
