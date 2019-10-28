package Pages;

import com.csvreader.CsvWriter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BasePage {
    public WebDriver webDriver;




    @Test
    public void TrendyolHome() throws IOException, InterruptedException {
        String workingDir = System.getProperty("user.dir");
        System.setProperty("webdriver.chrome.driver", workingDir + "/chromedriver");
        webDriver = new ChromeDriver();
        webDriver.get("https://trendyol.com");

        CsvWriter writer = getCsvFile("Results.csv");
        for (WebElement element : getElements()) {
            URL url = getURL(element.findElement(By.tagName("a")), "href");
            if (url != null) {
                checkStatus(writer, url);
            }
        }
        if (writer != null) {
            writer.close();
        }
        webDriver.close();

    }

    @Test
    public void TrendyolImages() throws IOException, InterruptedException {
        String workingDir = System.getProperty("user.dir");
        System.setProperty("webdriver.chrome.driver", workingDir + "/chromedriver");
        webDriver = new ChromeDriver();
        webDriver.get("https://trendyol.com");

        CsvWriter writer = getCsvFile("Images.csv");
        for (WebElement element : getElements()) {
            WebElement image = element.findElement(By.tagName("img"));
            URL imageUrl = getURL(image, "src");
            if (imageUrl != null) {
                checkImageConnectionStatus(writer, imageUrl);
            }
        }
        if (writer != null) {
            writer.close();
        }
        webDriver.close();

    }

    private List<WebElement> getElements() throws InterruptedException {
        List<WebElement> elements = webDriver.findElements(
                By.xpath("//div[@class='component-list component-big-list']//article[@class='component-item']"));

        // Big list'deki elementlerin sonuncusuna scoll ettirir.
        ((JavascriptExecutor) webDriver).executeScript(
                "arguments[0].scrollIntoView();", elements.get(elements.size() - 1));

        // Paging'in yuklenmesi icin 2 saniye bekliyoruz
        Thread.sleep(2000);

        List<WebElement> smallElements = webDriver.findElements(
                By.xpath("//div[@class='component-list component-small-list']//article[@class='component-item']"));

        if (smallElements != null && !smallElements.isEmpty()) {
            elements.addAll(smallElements);
        }
        return elements;
    }

    private URL getURL(WebElement element, String attributeName) {
        String url = element.getAttribute(attributeName);
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void checkStatus(CsvWriter writer, URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            writeTest(writer, url.getPath(), con.getResponseCode() + "");
        } catch (IOException e) {
            writeTest(writer, url.getPath(), "400");
        }
    }

    private void checkImageConnectionStatus(CsvWriter writer, URL url) {
        try {
            long currentTimeMillis = System.currentTimeMillis();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String responseBody = br.lines().collect(Collectors.joining());
            currentTimeMillis = System.currentTimeMillis() - currentTimeMillis;
            writeImageTestResult(writer, url.toString(), currentTimeMillis);
        } catch (IOException e) {
            writeTest(writer, url.getPath(), "400");
        }
    }

    private CsvWriter getCsvFile(String fileName) {
        boolean delete = new File(fileName).delete();
        try {
            CsvWriter writer = new CsvWriter(new FileWriter(fileName, true), ',');
            writeTest(writer, "TestUrl", "TestStatus");
            return writer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeTest(CsvWriter writer, String url, String status) {
        try {
            writer.write(url);
            writer.write(status);
            writer.endRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeImageTestResult(CsvWriter writer, String url, long duration) {
        try {
            writer.write(url);
            writer.write(duration + "");
            writer.endRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
