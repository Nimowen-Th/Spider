package com.nimowen;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpiderApplication
 *
 */
public class SpiderApplication
{
    //每页爬取的记录数
    private static int PAGE_SIZE = 20;
    //日志库配置
    private static Logger logger = LoggerFactory.getLogger(SpiderApplication.class);
    //影评url页数前缀
    private static final String BASE_URL = "https://movie.douban.com/subject/1292052/reviews?start=";

    /**
     * 爬取参数页面内容
     * @param url  请求网址
     */
    public void requestByUrl(String url) {
        //创建客户端对象
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        //创建http请求对象
        HttpGet httpGet = new HttpGet(url);
        //添加请求头
        httpGet.setHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36");

        try {
            //创建响应对象
            CloseableHttpResponse response = httpClient.execute(httpGet);
            //获取响应状态的状态行信息
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() != 200) {
                System.out.println("请求失败");
                return;
            }else {
                System.out.println("失败");
            }
            //获取请求数据
            HttpEntity httpEntity = response.getEntity();
            //httpEntity转为String
            String content = EntityUtils.toString(httpEntity);
            //解析当前页数据
            parseHtml(content);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseHtml(String content) {
        //转换成Document对象
        Document document = Jsoup.parse(content);

        //提取该页面20条影评信息
        Elements elements = document.select("div.article > div.review-list > div > div.main.review-item");

        //解析数据(影评标题,影评人,评分，发影评时间,有用，没用，回复)
        for(Element item: elements) {
            // 1. header
            Element header = item.selectFirst("header");
            //影评人
            String username = header.selectFirst("a.name").text();
            //评分
            String rating = header.selectFirst("span").attr("title");
            //发影评时间
            String date = header.selectFirst("span.main-meta").text();
            // 2. body
            Element body = item.selectFirst("div.main-bd");
            //标题
            String title = body.selectFirst("h2 > a").text();
            //评论的综合元素
            Element ratings = body.selectFirst("div.action");
            String usefulCount = ratings.selectFirst("a.action-btn.up > span").text();
            String uselessCount = ratings.selectFirst("a.action-btn.down > span").text();
            String replyCount = ratings.selectFirst("a.reply").text();


            //使用正则表达式
            Pattern pattern = Pattern.compile("\\d*");
            Matcher matcher = pattern.matcher(replyCount);
            replyCount = matcher.find() ? matcher.group() : "0";

            //String内容拼接
            StringBuilder str = new StringBuilder();
            str.append(title).append(",").append(username).append(",").append(rating).append(",")
                    .append(date).append(",").append(usefulCount).append(",").append(uselessCount).append(",")
                    .append(replyCount);

            //写入爬取到的内容到日志
            logger.info(str.toString());
        }
    }

    /**
     * 爬取n页内容
     */
    public void requestByPages(int page){
        int beginIndex = 0;
        for(int i = 0; i < page; ++i) {
            beginIndex = i * PAGE_SIZE;
            requestByUrl(BASE_URL + beginIndex);
            System.out.println();
        }
    }


    public static void main( String[] args ) {
        SpiderApplication spider = new SpiderApplication();
        //爬取一百页内容
        spider.requestByPages(100);
    }
}
