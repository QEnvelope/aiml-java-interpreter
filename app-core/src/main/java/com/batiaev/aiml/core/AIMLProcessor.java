package com.batiaev.aiml.core;

import com.batiaev.aiml.consts.AIMLConst;
import com.batiaev.aiml.consts.AIMLTag;
import com.batiaev.aiml.consts.WildCard;
import com.batiaev.aiml.entity.Category;
import com.batiaev.aiml.entity.CategoryList;
import com.batiaev.aiml.utils.XmlHelper;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The core AIML parser and interpreter.
 * Implements the AIML 2.0 specification as described in
 * AIML 2.0 Working Draft document
 * https://docs.google.com/document/d/1wNT25hJRyupcG51aO89UcQEiG-HkXRXusukADpFnDs4/pub
 * https://playground.pandorabots.com/en/tutorial/
 * http://blog.pandorabots.com/aiaas-aiml-2-0-support/
 * http://www.alicebot.org/documentation/aiml101.html
 * http://itc.ua/articles/kvest_tyuringa_7667/
 * http://www.eblong.com/zarf/markov/chan.c
 *
 * @author anton
 * @author Marco
 *         Implementation SET tag processing on 19/08/2016
 *         Topic managment improvement on 20/08/2016
 *         Parsing THINK tag
 *         Reimplemented isMatching method
 */
public class AIMLProcessor {
    private CategoryList categoryList;
    private HashMap<String, String> predicates;

    public AIMLProcessor(CategoryList categories) {
        this.predicates = new HashMap<>();
        this.categoryList = categories;
    }

    public String match(String input, String topic, String that) {
        Set<String> patterns = categoryList.patterns(topic);
        for (String pattern : patterns) {
            if (isMatching(input.toUpperCase(), pattern))
                return pattern;
        }
        patterns = categoryList.patterns(AIMLConst.default_topic);
        for (String pattern : patterns) {
            if (isMatching(input.toUpperCase(), pattern))
                return pattern;
        }
        return WildCard.sumbol_1more;
    }

    private boolean isMatching(String input, String pattern) {
        input = input.trim();
        String regex_pattern = pattern.trim();
        regex_pattern = regex_pattern.replace(WildCard.sumbol_1more, ".+");
        regex_pattern = regex_pattern.replace(WildCard.sumbol_1more_higest, ".+");
        regex_pattern = regex_pattern.replace(WildCard.sumbol_0more, ".*");
        regex_pattern = regex_pattern.replace(WildCard.sumbol_0more_higest, ".*");
        regex_pattern = regex_pattern.replace(" ", "\\s*");
        return Pattern.matches(regex_pattern, input);
    }

    public String getCategoryValue(Node node) {
        if (node == null)
            return AIMLConst.default_bot_response;

        String result = "";
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            if (childNodes.item(i).getNodeName().equals(AIMLTag.template))
                result = getTemplateValue(childNodes.item(i));
        }
        return result.isEmpty() ? AIMLConst.default_bot_response : result;
    }

    public String getTemplateValue(Node node) {
        String result = "";
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) result += recurseParse(childNodes.item(i));
        return result.isEmpty() ? AIMLConst.default_bot_response : result;
    }

    private String recurseParse(Node node) {
        String nodeName = node.getNodeName();
        switch (nodeName) {
            case AIMLTag.text:
                return node.getNodeValue().replaceAll("(\r\n|\n\r|\r|\n)", "").replaceAll("  ", " ");
            case AIMLTag.template:
                return getTemplateValue(node);
            case AIMLTag.random:
                return randomParse(node);
            case AIMLTag.srai:
                return sraiParse(node);
            case AIMLTag.set:
                setParse(node);
                return "";
            case AIMLTag.think:
                getTemplateValue(node);
                return "";
        }
        return "";
    }

    private void setParse(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes.getLength() > 0) {
            Node node1 = attributes.getNamedItem("getName");
            String key = node1.getNodeValue();
            String value = getTemplateValue(node);
            predicates.put(key, value);
        }
    }

    private String sraiParse(Node node) {
        Category category = categoryList.category(AIMLConst.default_topic, XmlHelper.node2String(node));
        return category != null ? getCategoryValue(category.getNode()) : AIMLConst.error_bot_response;
    }

    private String randomParse(Node node) {
        ArrayList<String> values = new ArrayList<>();
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            if (childNodes.item(i).getNodeName().equals(AIMLTag.li))
                values.add(XmlHelper.node2String(childNodes.item(i)));
        }

        Random rn = new Random();
        int index = rn.nextInt(values.size());
        return values.get(index);
    }

    public String template(String pattern, String topic, String that, HashMap<String, String> predicates) {
        this.predicates = predicates;
        Category category = categoryList.category(topic, pattern);
        if (category == null)
            category = categoryList.category(AIMLConst.default_topic, WildCard.sumbol_1more);
        return category == null ? AIMLConst.default_bot_response : getCategoryValue(category.getNode());
    }

    public int getTopicCount() {
        return categoryList.topicCount();
    }

    public int getCategoriesCount() {
        return categoryList.size();
    }
}