package com.core.service;

import com.core.config.Config;
import com.core.controller.ArticleController;
import com.core.domain.*;
import com.core.util.Constant;
import com.google.gson.JsonObject;
import org.apache.commons.collections.map.HashedMap;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolManager;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yk on 2017/4/24.
 */
@Service
public class HomePageService extends BaseService {

    @Autowired
    private ToolManager toolManager;

    @Autowired
    private Config config;

    @Autowired
    private HeadLineService headLineService;

    private JsonObject jsonObject = new JsonObject();

    /**
     * 静态化 首页
     */
    public ObjectNode staticIndex(int userId) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        /**
         * 结合后台--待完善目标
         * 1、模版路径根据template表中数据获取
         * 2、各模块数据获取
         * 3、获取登录的admin信息
         * 4、其他细节
         */

        long publishId = createPublishLog(userId, "首页静态化");
        PublishLog publishLog = find(PublishLog.class, publishId);

        String staticResPrefix = config.getStaticResourceURLPrefix();
        String listDomain = config.getListDomain();

        ToolContext toolManagerContext = toolManager.createContext();

        //返回绑定的文件路径
        toolManagerContext.put("resURLPrefix", staticResPrefix);
        toolManagerContext.put("listURLPrefix", listDomain);
        toolManagerContext.put("winTitle", "首页_" + config.getProjectName());
        toolManagerContext.put("depict", "首页,描述");

        try {

            // 焦点图
            Map<String, Object> focus = staticOtherModules(Constant.CATEGORY_ID_FOCUS, Constant.GENERAL_ID_TWO);
            toolManagerContext.put("focus", focus);

            // 新闻法讯
            Map<String, Object> news = staticOtherModules(Constant.CATEGORY_ID_NEWS, Constant.GENERAL_ID_ONE);
            toolManagerContext.put("news", news);

            // 生活禅
            Map<String, Object> life = staticOtherModules(Constant.CATEGORY_ID_LIFE, Constant.GENERAL_ID_TWO);
            toolManagerContext.put("life", life);

            // 紫云佛国
            Map<String, Object> ziyun = staticOtherModules(Constant.CATEGORY_ID_ZIYUNFOGUO, Constant.GENERAL_ID_TWO);
            toolManagerContext.put("ziyun", ziyun);

            // 佛教常识
            Map<String, Object> knowledge = staticOtherModules(Constant.CATEGORY_ID_KNOWLEDGE, Constant.GENERAL_ID_TWO);
            toolManagerContext.put("knowledge", knowledge);

            // 禅医养生
            Map<String, Object> medical = staticOtherModules(Constant.CATEGORY_ID_MEDICAL, Constant.GENERAL_ID_TWO);
            toolManagerContext.put("medical", medical);

            // 藏经阁
            Map<String, Object> depository = staticOtherModules(Constant.CATEGORY_ID_DEPOSITORY, Constant.GENERAL_ID_ONE);
            toolManagerContext.put("depository", depository);

            // 水墨禅韵
            Map<String, Object> waterzen = staticOtherModules(Constant.CATEGORY_ID_WATERZEN, Constant.GENERAL_ID_TWO);
            toolManagerContext.put("waterzen", waterzen);

            // 导航
            staticNav();

            // 广种福田、联系我们
            staticFutian();

            // 紫云法务
            staticFawu();

            create("/index.html", "base/index.vm", toolManagerContext, null);

            publishLog.setFinishDate(new Date());
            update(publishLog);
            objectNode.put("success", true);

        } catch (Exception e) {
            publishLog.setStatus(0);
            publishLog.setFinishDate(new Date());
            update(publishLog);
            objectNode.put("success", false);
        }

        return objectNode;
    }

    /**
     * 静态化 导航
     */
    public ObjectNode staticNav() {
        ObjectNode objectNode = objectMapper.createObjectNode();
        long id = createPublishLog(1, "导航");
        PublishLog publishLog = find(PublishLog.class, id);
        try {
            String[] nav;
            ToolContext toolManagerContext = toolManager.createContext();
            nav = searchNav("main");
            toolManagerContext.put("main", nav);
            nav = searchNav("deputy");
            toolManagerContext.put("deputy", nav);
            create("/base/nav.html", "base/nav.vm", toolManagerContext, null);
            objectNode.put("success", true);
        } catch (Exception e) {
            publishLog.setStatus(0);
            objectNode.put("success", false);
        }
        publishLog.setFinishDate(new Date());
        update(publishLog);
        return objectNode;
    }

    /**
     * 静态化 广种福田、联系我们
     */
    public void staticFutian() {
        long id = createPublishLog(1, "广种福田、联系我们");
        PublishLog publishLog = find(PublishLog.class, id);
        try {
            String[] resultFutian = searchFutian();
            List<String[]> resultContact = searchContact();

            ToolContext toolManagerContext = toolManager.createContext();
            toolManagerContext.put("futian", resultFutian);
            toolManagerContext.put("contact", resultContact);

            create("/base/futian.html", "base/futian.vm", toolManagerContext, null);

        } catch (Exception e) {
            publishLog.setStatus(0);
        }
        publishLog.setFinishDate(new Date());
        update(publishLog);

    }

    /**
     * 静态化 紫云法务
     */
    public void staticFawu() {
        long id = createPublishLog(1, "紫云法务");
        PublishLog publishLog = find(PublishLog.class, id);

        try {
            Map map = new HashedMap();
            List<String[]> res = new ArrayList<String[]>();

            String name = find(Category.class, Constant.CATEGORY_ID_LAW).geteName();
            map.put("name", name);

            List<String[]> resultList = searchFawu();
            for (int i = 0; i < resultList.size(); i++) {
                String[] result = resultList.get(i);

                String title = result.length >= 0 ? result[0] : "";
                if (title.length() > 5) {
                    title = title.substring(0, title.length() >= 4 ? 4 : title.length()) + "...";
                    result[0] = title;
                }

                String content = result.length >= 3 ? result[2] : "";
                content = contentReplace(content);
                content = content.substring(0, content.length() > 270 ? 270 : content.length()) + "...";
                result[2] = content;
                res.add(i, result);
            }

            ToolContext toolManagerContext = toolManager.createContext();
            toolManagerContext.put("law", map);
            toolManagerContext.put("content", res);

            create("/base/fawu.html", "base/fawu.vm", toolManagerContext, null);

        } catch (Exception e) {
            publishLog.setStatus(0);
        }

        publishLog.setFinishDate(new Date());
        update(publishLog);

    }

    /**
     * 静态化 广告位
     */
    public void staticAd() {
        long id = createPublishLog(1, "广告位");
        PublishLog publishLog = find(PublishLog.class, id);
        try {
            List<Ad> resultAdList = searchAd();

            ToolContext toolManagerContext = toolManager.createContext();
            toolManagerContext.put("ad", resultAdList);

            create("/base/banner.html", "base/banner.vm", toolManagerContext, null);

        } catch (Exception e) {
            publishLog.setStatus(0);
        }
        publishLog.setFinishDate(new Date());
        update(publishLog);

    }

    /**
     * 静态化 活动通知
     */
    public void staticNotice() {
        long id = createPublishLog(1, "活动通知");
        PublishLog publishLog = find(PublishLog.class, id);
        try {
            List<Article> resultAdList = searchNotice();
            List<String[]> list = new ArrayList<String[]>();
            ToolContext toolManagerContext = toolManager.createContext();

            for (int i = 0; i < resultAdList.size(); i++) {
                Article article = resultAdList.get(i);
                String[] items = new String[2];
                items[0] = article.getUrl();
                items[1] = article.getTitle();
                list.add(i, items);
            }

            toolManagerContext.put("notice", list);

            create("/base/notice.html", "base/notice.vm", toolManagerContext, null);

        } catch (Exception e) {
            publishLog.setStatus(0);
        }

        publishLog.setFinishDate(new Date());
        update(publishLog);
    }

    /**
     * 发布文章
     *
     * @param id
     * @return
     */
    public String articlePublish(long id, String type) {
        String templatePath = null;
        String articlePath = null;
        String staticResPrefix = config.getStaticResourceURLPrefix();

        ToolContext toolManagerContext = toolManager.createContext();

        //返回绑定的文件路径
        toolManagerContext.put("resURLPrefix", staticResPrefix);

        Article article = find(Article.class, id);
        article.setPublishDate(new Date());
        if (null != article) {

            Map<String, Object> params = new HashedMap();
            params.put("aId", article.getId());
            List<ArticleKeyWord> articleKeyWords = list(ArticleKeyWord.class, "WHERE aId = :aId ORDER BY orderBy ASC", params);
            for (int i = 0; i < articleKeyWords.size(); i++) {
                ArticleKeyWord articleKeyWord = articleKeyWords.get(i);
                KeyWord keyWord = find(KeyWord.class, articleKeyWord.getkId());
                toolManagerContext.put("keyWord", keyWord.getName());
            }
            toolManagerContext.put("winTitle", article.getTitle());
            toolManagerContext.put("depict", article.getDepict());

            Category category = find(Category.class, article.getcId());
            articlePath = category.getName();
            Map<String, Object> map = getStaticArticleInfoMap(article);
            map.put("showCategory", category.geteName());
            map.put("category", category.geteName());
            map.put("categoryUrl", "栏目list链接");
            map.put("publishDate", new SimpleDateFormat("yyyy-MM-dd").format(article.getPublishDate()));

            Template template = find(Template.class, category.gettAId());
            templatePath = "/base/" + template.getFileName();

            toolManagerContext.put("article", map);
            articlePath = "/base/" + articlePath + "/" + article.getId() + ".html";
            if (null != type && "preview".equals(type)) {
                articlePath = config.getPreviewDir() + "/" + article.getId() + ".html";
                create(articlePath, templatePath, toolManagerContext, "preview");
            } else {
                create(articlePath, templatePath, toolManagerContext, null);
            }


            if (null != type && "preview".equals(type)) {
                String path = config.getDomain();
                path = path.substring(0, path.lastIndexOf("/") + 1) + "cn/article/preview?id=" + id;
                return path;
            } else {
                return staticResPrefix + "/" + articlePath;
            }
        }
        return null;
    }

    /**
     * 创建静态页面
     *
     * @param create:静态文件放置路径
     * @param templatePath：模版文件路径
     * @param toolManagerContext：绑定的数据
     */
    private String create(String create, String templatePath, ToolContext toolManagerContext, String type) {

        String createPath = config.getArticleDir();
        if (null != type && "preview".equals(type)) {
            createPath = "";
        }
        String checkPath = create.substring(0, create.lastIndexOf("/"));
        fileCheck(createPath, checkPath);
        String flag = "success";
        VelocityEngine velocityEngine;
        PrintWriter printWriter;
        try {
            velocityEngine = toolManager.getVelocityEngine();
            printWriter = new PrintWriter(createPath + create, Constant.ENCODE_UTF8);
            velocityEngine.mergeTemplate(templatePath, Constant.ENCODE_UTF8, toolManagerContext, printWriter);

            printWriter.flush();
        } catch (Exception e) {
            flag = "error";
            jsonObject.addProperty("message", e.getMessage());
        }
        jsonObject.addProperty("flag", flag);
        return jsonObject.toString();
    }

    /**
     * 获取其他模块Map
     */
    private Map<String, Object> staticOtherModules(int articleId, int different) {
        String titleName = articleId == Constant.CATEGORY_ID_FOCUS ? null : getCategoryEName(articleId);
        Map<String, Object> map = new HashMap<String, Object>();
        List<String[]> list;
        HeadLine headLine;
        Article article;
        map.put("name", titleName);
        String[] news;
        List<String[]> other;
        int maxLimit = 4;
        if (articleId == Constant.CATEGORY_ID_NEWS) {
            maxLimit = 9;
        } else if (articleId == Constant.CATEGORY_ID_DEPOSITORY) {
            maxLimit = 7;
        }
        List<HeadLine> headLines = headLineService.searchHeadLine(articleId, different, maxLimit);
        switch (articleId) {
            case Constant.CATEGORY_ID_NEWS:
                // 新闻法讯 -- 文字头条
                news = new String[3];
                other = new ArrayList<String[]>();
                headLine = headLines.size() > 0 ? headLines.get(0) : null;
                if (null != headLine && headLine.getId() > 0) {
                    article = find(Article.class, headLine.getaId());
                    if (null != article) {
                        news[0] = article.getUrl();
                        news[1] = headLine.getName();
                        news[2] = article.getDepict();
                    }
                    map.put("first", news);
                }
                for (int i = 1; i < headLines.size(); i++) {
                    news = new String[2];
                    HeadLine headLineOther = headLines.get(i);
                    article = find(Article.class, headLine.getaId());
                    if (null != article) {
                        news[0] = article.getUrl();
                        news[1] = headLineOther.getName();
//                    news[2] = article.getDepict();
                    }
                    other.add(i - 1, news);
                }
                map.put("other", other);
                return map;
            case Constant.CATEGORY_ID_LIFE:
                // 生活禅  limit 0,4
            case Constant.CATEGORY_ID_ZIYUNFOGUO:
                // 紫云佛国  limit 0,4
            case Constant.CATEGORY_ID_KNOWLEDGE:
                // 佛教常识  limit 0,4
            case Constant.CATEGORY_ID_MEDICAL:
                // 禅医养生  limit 0,4
                // 文章链接，头条标题，文章描述，文章日期，头条图片
                news = new String[5];
                headLine = headLines.size() > 0 ? headLines.get(0) : null;
                if (null != headLine && headLine.getId() > 0) {
                    article = find(Article.class, headLine.getaId());
                    Media media = find(Media.class, headLine.getmId());
                    if (null != article) {
                        news[0] = article.getUrl();
                        news[1] = headLine.getName();
                        news[2] = article.getDepict();
                    }
                    news[3] = null != article.getUpdateDate() ? getArticleDate(article.getUpdateDate()) : getArticleDate(article.getCreateDate());
                    news[4] = null != media ? media.getUrl() : null;
                }
                map.put("first", news);
                headLines = headLineService.searchHeadLine(articleId, Constant.GENERAL_ID_ONE, maxLimit);
                other = new ArrayList<String[]>();
                for (int i = 0; i < headLines.size(); i++) {
                    news = new String[3];
                    headLine = headLines.get(i);
                    article = find(Article.class, headLine.getaId());
                    if (null != article) {
                        news[0] = article.getUrl();
                        news[1] = headLine.getName();
                    }
                    news[2] = null != article.getUpdateDate() ? getArticleDate(article.getUpdateDate()) : getArticleDate(article.getCreateDate());
                    other.add(i, news);
                }
                map.put("other", other);
                return map;
            case Constant.CATEGORY_ID_DEPOSITORY:
                // 藏经阁
                list = new ArrayList<String[]>();
                for (int i = 0; i < headLines.size(); i++) {
                    headLine = headLines.get(i);
                    article = find(Article.class, headLine.getaId());
                    String[] items = new String[2];
                    if (null != article) {
                        items[0] = article.getUrl();
                        items[1] = headLine.getName();
                    }
                    list.add(i, items);
                }
                map.put("list", list);
                return map;
            case Constant.CATEGORY_ID_WATERZEN:
                // 水墨禅韵 limit 0,4
                list = new ArrayList<String[]>();
                for (int i = 0; i < headLines.size(); i++) {
                    news = new String[3];
                    headLine = headLines.get(i);
                    article = find(Article.class, headLine.getaId());
                    Media media = find(Media.class, headLine.getmId());
                    if (null != article) {
                        news[0] = headLine.getName();
                        news[1] = article.getUrl();
                        news[2] = media.getUrl();
                    }
                    list.add(news);
                }
                map.put("list", list);
                return map;
            case Constant.CATEGORY_ID_FOCUS:
                // 焦点图
                list = new ArrayList<String[]>();
                for (int i = 0; i < headLines.size(); i++) {
                    headLine = headLines.get(i);
                    article = find(Article.class, headLine.getaId());
                    Media media = find(Media.class, headLine.getmId());
                    news = new String[4];
                    if (null != article) {
                        news[0] = headLine.getName();
                        news[1] = article.getUrl();
                        news[2] = media.getUrl();
                        news[3] = media.getUrl();
                    }
                    list.add(i, news);
                }

                map.put("list", list);
                return map;
            default:
                return null;
        }
    }

    /**
     * 检查文件夹是否存在
     *
     * @param path
     */
    private void fileCheck(String path, String folderName) {
        path += File.separator + folderName;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /* ==========================获取静态化数据=============================*/

    /**
     * 静态化文章 封装数据
     *
     * @param article
     * @return
     */
    public Map<String, Object> getStaticArticleInfoMap(Article article) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        sb.append(article.getContent());
        params.put("aId", article.getId());
        List<SubArticle> subArticles = list(SubArticle.class, "WHERE aId = :aId ORDER BY seq ASC", params);
        for (int i = 0; i < subArticles.size(); i++) {
            SubArticle subArticle = subArticles.get(i);
            sb.append(subArticle.getContent());
        }
        map.put("title", article.getTitle());
        map.put("publishDate", article.getPublishDate());
        map.put("source", article.getSource());
        map.put("author", article.getAuthor());
        map.put("content", sb.toString());
        map.put("categoryId", article.getcId());
        return map;
    }


    /**
     * 获取主/副导航数据
     *
     * @return
     */
    private String[] searchNav(String type) {
        int articleId;
        if ("main".equals(type)) {
            articleId = Constant.NAV_ID_MAIN;
        } else {
            articleId = Constant.NAV_ID_DEPUTY;
        }
        StringBuffer sb = new StringBuffer();
        Article article = find(Article.class, articleId);
        sb.append(article.getContent());

        Map<String, Object> map = new HashedMap();
        map.put("aId", article.getId());

        List<SubArticle> subArticles = list(SubArticle.class, " WHERE aId=:aId ORDER BY seq ASC", map);

        for (SubArticle subList : subArticles) {
            sb.append(subList.getContent());
        }

        String[] results = sb.toString().split(",");

        return results;
    }

    /**
     * 获取广种福田数据
     *
     * @return
     */
    private String[] searchFutian() {
        StringBuffer sb = new StringBuffer();
        Map<String, Object> articleMap = new HashedMap();
        articleMap.put("cId", Constant.CATEGORY_ID_FUTIAN + "");

        Article article = list(Article.class, " WHERE cId = :cId", articleMap).get(0);

        sb.append(article.getTitle()).append(Constant.ARTICE_CONTENT_SPLICE).append(article.getContent());

        Map<String, Object> map = new HashedMap();
        map.put("aId", article.getId());

        List<SubArticle> subArticles = list(SubArticle.class, " WHERE aId=:aId ORDER BY seq ASC", map);

        for (SubArticle subList : subArticles) {
            sb.append(subList.getContent());
        }

        // 广种福田
        String[] content = sb.toString().split(Constant.ARTICE_CONTENT_SPLICE);
        String[] result = new String[content.length];
        for (int i = 0; i < result.length; i++) {
            if (i < content.length) {
                result[i] = HtmlUtils.htmlUnescape(content[i].trim());
            } else {
                result[i] = "";
            }
        }

        return result;
    }

    /**
     * 获取 联系我们 数据
     *
     * @return
     */
    private List<String[]> searchContact() {
        StringBuffer sb = new StringBuffer();
        Map<String, Object> articleMap = new HashedMap();
        articleMap.put("cId", Constant.CATEGORY_ID_CONTACT + "");
        Article article = list(Article.class, " WHERE cId = :cId", articleMap).get(0);

        sb.append(article.getContent());

        Map<String, Object> map = new HashedMap();
        map.put("aId", article.getId());

        List<SubArticle> subArticles = list(SubArticle.class, " WHERE aId=:aId ORDER BY seq ASC", map);

        for (SubArticle subList : subArticles) {
            sb.append(subList.getContent());
        }

        String[] content = sb.toString().split(Constant.ARTICE_CONTENT_SPLICE);
        List<String[]> resultList = new ArrayList<String[]>();
        String[] result;
        for (int i = 0; i < content.length; i++) {
            result = new String[2];
            String str = content[i];
            if (str.contains(",")) {
                // 包含 ,
                result[0] = str.split(",")[0].trim();
                result[1] = str.split(",")[1].trim();
            } else {
                result[0] = str.trim();
                result[1] = "";
            }

            resultList.add(result);
        }

        return resultList;
    }

    /**
     * 获取 紫云法务
     *
     * @return
     */
    private List<String[]> searchFawu() {
        List<String[]> resultList = new ArrayList<String[]>();

        Map<String, Object> articleMap = new HashedMap();
        articleMap.put("cId", Constant.CATEGORY_ID_LAW);
        List<HeadLine> headLineList = headLineService.searchHeadLine(Constant.CATEGORY_ID_LAW, Constant.GENERAL_ID_ONE, 3);

        for (int i = 0; i < headLineList.size(); i++) {
            StringBuffer contentSb = new StringBuffer();
            String[] result = new String[3];
            Map<String, Object> subMap = new HashedMap();
            HeadLine headLine = headLineList.get(i);
            Article article = find(Article.class, headLine.getaId());
            result[0] = headLine.getName();
            result[1] = article.getUrl().contains("http") ? article.getUrl() : "";
            contentSb.append(article.getContent());

            subMap.put("aId", headLine.getaId());
            List<SubArticle> subArticleList = list(SubArticle.class, " WHERE aId=:aId ORDER BY seq ASC", subMap);

            for (int i1 = 0; i1 < subArticleList.size(); i1++) {

                SubArticle subArticle = subArticleList.get(i1);
                contentSb.append(subArticle.getContent());
            }
            result[2] = contentSb.toString();
            resultList.add(i, result);
        }
        return resultList;
    }

    /**
     * 获取 广告位
     *
     * @return
     */
    private List<Ad> searchAd() {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("status", Constant.ARTICLE_ID_NINE);
        List<Ad> adList = list(Ad.class, " WHERE status = :status ORDER BY id ASC", paramMap);
        return adList.size() >= 2 ? adList : null;
    }

    /**
     * 获取 活动通知
     *
     * @return
     */
    private List<Article> searchNotice() {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("status", Constant.ARTICLE_ID_NINE);
        paramMap.put("cId", Constant.CATEGORY_ID_NOTICE);
        List<Article> adList = list(Article.class, " WHERE status = :status AND cId = :cId ORDER BY orderBy ASC", paramMap);
        return adList.size() > 0 ? adList : null;
    }

    /**
     * 获取导航初始数据
     *
     * @param type
     * @return
     */
    public String[] searchNavReset(String type) {
        String[] navs = null;
        String templatePath = config.getArticleDir() + File.separator
                + "static" + File.separator + "html" + File.separator;

        String fileName = "";

        if ("main".equals(type)) {
            fileName = "nav.txt";
            navs = new String[8];
        } else if ("deputy".equals(type)) {
            fileName = "deputy.txt";
            navs = new String[3];
        }
        String filePath = templatePath + fileName;
        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String row;
            int line = 0;
            while ((row = reader.readLine()) != null) {
                navs[line] = row;
                line++;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return navs;
    }

    /**
     * 获取分类中文名称
     *
     * @param id
     * @return
     */
    private String getCategoryEName(int id) {
        return find(Category.class, id).geteName();
    }

    /**
     * 文章时间格式
     *
     * @param date
     * @return
     */
    private String getArticleDate(Date date) {
        return new SimpleDateFormat("MM-dd").format(date);
    }

    /**
     * 记录日志
     *
     * @param content
     */
    private long createPublishLog(int userId, String content) {
        PublishLog publishLog = new PublishLog();
        publishLog.setUserId(userId);
        publishLog.setCategory(content);
        publishLog.setStatus(1);
        publishLog.setRequestDate(new Date());

        return create(publishLog);
    }

    /**
     * 去掉文章中的空格换行符
     *
     * @param content
     * @return
     */
    public String contentReplace(String content) {
        Pattern p = Pattern.compile("\\s*|\t|\r|\n|<br />");
        Matcher m = p.matcher(content);
        content = m.replaceAll("");
        return content;
    }

}
