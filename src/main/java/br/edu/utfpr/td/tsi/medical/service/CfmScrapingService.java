package br.edu.utfpr.td.tsi.medical.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CfmScrapingService {
    private static final String URL_BUSCA = "https://portal.cfm.org.br/busca-medicos/";
    private final ObjectMapper leitorJson;

    public CfmScrapingService(ObjectMapper leitorJson) {
        this.leitorJson = leitorJson;
    }

    public Map<String, JsonNode> buscar(List<String> estados) throws JsonProcessingException {

        ChromeOptions opcoes = new ChromeOptions();
        opcoes.addArguments("--start-maximized");
        WebDriver navegador = new ChromeDriver(opcoes);
        Map<String, JsonNode> respostas = new LinkedHashMap<>();

        try {
            navegador.get(URL_BUSCA);
            JavascriptExecutor javascript = (JavascriptExecutor) navegador;
            new WebDriverWait(navegador, Duration.ofSeconds(45))
                    .until(driver -> Boolean.TRUE.equals(javascript.executeScript(
                            "return !!window.jQuery && !!document.querySelector('#uf');")));

            javascript.executeScript("""
                    window.__respostasCfm = [];
                    jQuery(document).ajaxSuccess(function(evento, requisicao, opcoes, resposta) {
                        if (opcoes.url.includes('/medicos/buscar_medicos')) {
                            window.__respostasCfm.push(JSON.stringify(resposta));
                        }
                    });
            """);

            for (String uf : estados) {
                new WebDriverWait(navegador, Duration.ofSeconds(45))
                        .until(driver -> !driver.findElements(By.cssSelector("#uf option[value='" + uf + "']")).isEmpty());

                new Select(navegador.findElement(By.id("uf"))).selectByValue(uf);
                javascript.executeScript("window.__respostasCfm = [];");
                navegador.findElement(By.cssSelector("#buscaForm button[type='submit']")).click();

                String json = new WebDriverWait(navegador, Duration.ofMinutes(3)).until(driver -> {
                    Object valor = javascript.executeScript("return window.__respostasCfm.length ? window.__respostasCfm.shift() : null;");
                    if (valor == null || "\"expirou\"".equals(valor) || "\"invalidinput\"".equals(valor)) {
                        return null;
                    }
                    return valor.toString();
                });
                JsonNode resposta = leitorJson.readTree(json);
                if (!resposta.path("dados").isArray()) {
                    throw new IllegalStateException("Resposta inesperada do CFM para " + uf);
                }
                respostas.put(uf, resposta);
            }
            return respostas;
        } finally {
            navegador.quit();
        }
    }
}
