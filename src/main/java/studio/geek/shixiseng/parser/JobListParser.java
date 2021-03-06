package studio.geek.shixiseng.parser;

/**
 * Created by Liuqi
 * Date: 2017/4/8.
 */

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import studio.geek.util.RegexUtil;
import studio.geek.util.SimpleLogger;
import studio.geek.shixiseng.entity.Company;
import studio.geek.shixiseng.entity.Job;
import studio.geek.shixiseng.entity.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 解析职位列表页信息
 */
public class JobListParser {
    Logger logger = SimpleLogger.getSimpleLogger(JobListParser.class);
    private static volatile JobListParser jobListParser;

    public static JobListParser getInstance() {
        if (jobListParser == null) {
            synchronized (JobListParser.class) {
                if (jobListParser == null) {
                    jobListParser = new JobListParser();
                }
            }
        }
        return jobListParser;
    }

    private JobListParser() {
    }

    /**
     * 解析列表页成一个job的list,但是能够提取的数据有限
     *
     * @param page
     * @return
     */
    public List<Job> parserJobList(Page page) throws Exception {
        List<Job> jobList = new ArrayList<Job>();
        if (page.getStatusCode() != 200) {
            System.out.println("-----访问页面出错" + page.getStatusCode());
            return null;
        }
        Document doc = Jsoup.parse(page.getHtml());
        Elements jobs = doc.getElementsByClass("job_inf_inf");
        for (Element jobElement : jobs) {
            jobList.add(doParser(jobElement));
        }
        return jobList;
    }

    /**
     * 通过列表页，进入到每一项工作职位的详情页，解析
     *
     * @param page 列表页
     * @return
     */
    public List<Job> parserJobListForDetail(Page page) {
        String baseUrl = "http://www.shixiseng.com";
        List<Job> jobList = new LinkedList<Job>();
        JobDetailPageParser jobDetailPageParser = JobDetailPageParser.getInstance();
        System.out.println(page.getUrl());

        Document doc = Jsoup.parse(page.getHtml());
        System.out.println("base url---" + doc.baseUri());
        Elements jobInfElements = doc.getElementsByClass("job_inf_inf");
        for (Element ele : jobInfElements) {
            Element element = ele.getElementsByTag("a").get(0);
            String url = baseUrl + element.attr("href");

            try {
                Document detailDoc = HttpConnection.connect(url).get();
               // System.out.println(detailDoc);
                jobList.add(jobDetailPageParser.parserJobDetail(detailDoc));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jobList;
    }

    /**
     * 解析job列表页并且持久化到数据库
     *
     * @param page
     * @return
     */
    public List<Job> parserJobListAndPresistence(Page page) {
        if (page.getStatusCode() != 200) {
            System.out.println("-----访问页面出错" + page.getStatusCode());
            return null;
        }
        return null;
    }

    /**
     * 解析一条列表页的一条job element
     *
     * @param jobElement
     * @return
     */
    private static Job doParser(Element jobElement) throws Exception {
        Job job = new Job();
        Company company = new Company();
        Elements elements = jobElement.getElementsByTag("a");
        // Element jobHead = jobElement.getElementsByClass("job_head").get(0);
        Element jobHead = elements.get(0);
        job.setIdentity(jobHead.attr("href"));
        job.setJobName(jobHead.attr("title"));
        Element companyEle = elements.get(1);
        company.setName(companyEle.attr("title"));
        company.setUrl(companyEle.attr("href"));
        job.setCompany(company);
        Elements detailElements = jobElement.getElementsByTag("span");
        job.setCity(detailElements.get(1).attr("title"));
        int lowSalary = 0;
        int highSalary = 0;
        String[] salary = RegexUtil.getStringByRegex(detailElements.get(3).toString(), "[\\d]+(-)[\\d]+").split("-");
        if (salary.length == 2) {
            lowSalary = Integer.parseInt(salary[0]);
            highSalary = Integer.parseInt(salary[1]);
        } else if (salary.length == 1) {
            highSalary = Integer.parseInt(salary[0]);
        }
        job.setLowSalary(lowSalary);
        job.setHighSalary(highSalary);
        return job;
    }
}
