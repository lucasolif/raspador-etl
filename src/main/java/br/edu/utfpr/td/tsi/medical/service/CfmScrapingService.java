package br.edu.utfpr.td.tsi.medical.service;

import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
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

    public void buscar(List<String> estados, BiConsumer<String, JsonNode> aoReceberPagina) throws JsonProcessingException {

        ChromeOptions opcoes = new ChromeOptions();
        opcoes.addArguments("--start-maximized");
        WebDriver navegador = new ChromeDriver(opcoes);
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
                            const parametros = JSON.parse(opcoes.data)[0];
                            const esperada = window.__consultaEsperadaCfm;
                            if (esperada && parametros.medico.ufMedico === esperada.uf
                                    && Number(parametros.pageNumber) === esperada.pagina
                                    && resposta !== 'expirou' && resposta !== 'invalidinput') {
                                window.__tamanhoPaginaCfm = Number(parametros.pageSize);
                                window.__respostasCfm.push(JSON.stringify(resposta));
                            }
                        }
                    });
            """);

            for (String uf : estados) {
                new WebDriverWait(navegador, Duration.ofSeconds(45))
                        .until(driver -> !driver.findElements(By.cssSelector("#uf option[value='" + uf + "']")).isEmpty());

                new Select(navegador.findElement(By.id("uf"))).selectByValue(uf);
                javascript.executeScript("window.__respostasCfm = []; window.currentPage = ''; window.__consultaEsperadaCfm = {uf: arguments[0], pagina: 1};", uf);
                navegador.findElement(By.cssSelector("#buscaForm button[type='submit']")).click();

                JsonNode primeiraPagina = aguardarPagina(navegador, javascript, uf, 1);
                long total = primeiraPagina.path("dados").get(0).path("COUNT").asLong(-1);
                if (total < 1) {
                    throw new IllegalStateException("Total de registros ausente na resposta do CFM para " + uf);
                }

                int tamanhoPagina = ((Number) javascript.executeScript("return window.__tamanhoPaginaCfm;")).intValue();
                if (tamanhoPagina < 1) {
                    throw new IllegalStateException("Paginacao inesperada do CFM para " + uf);
                }
                long totalPaginas = (total + tamanhoPagina - 1) / tamanhoPagina;
                if (totalPaginas > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Paginacao inesperada do CFM para " + uf);
                }

                long recebidos = primeiraPagina.path("dados").size();
                aoReceberPagina.accept(uf, primeiraPagina);

                for (long numeroPagina = 2; numeroPagina <= totalPaginas; numeroPagina++) {
                    int pagina = (int) numeroPagina;
                    javascript.executeScript("window.__respostasCfm = []; window.__consultaEsperadaCfm = {uf: arguments[0], pagina: arguments[1]};", uf, pagina);
                    javascript.executeScript("jQuery('#paginacao').pagination('go', arguments[0]);", pagina);
                    JsonNode resposta = aguardarPagina(navegador, javascript, uf, pagina);
                    recebidos += resposta.path("dados").size();
                    aoReceberPagina.accept(uf, resposta);
                }

                if (recebidos != total) {
                    throw new IllegalStateException("Consulta incompleta do CFM para " + uf
                            + ": esperados " + total + " registros, recebidos " + recebidos);
                }
            }
        } finally {
            navegador.quit();
        }
    }

    private JsonNode aguardarPagina(WebDriver navegador, JavascriptExecutor javascript, String uf, int pagina)
            throws JsonProcessingException {
        String json;
        try {
            json = new WebDriverWait(navegador, Duration.ofMinutes(3)).until(driver -> {
                Object valor = javascript.executeScript("return window.__respostasCfm.length ? window.__respostasCfm.shift() : null;");
                return valor == null ? null : valor.toString();
            });
        } catch (TimeoutException erro) {
            throw new IllegalStateException("Tempo limite aguardando a pagina " + pagina + " do CFM para " + uf, erro);
        }

        JsonNode resposta = leitorJson.readTree(json);
        if (!resposta.path("dados").isArray() || resposta.path("dados").isEmpty()) {
            throw new IllegalStateException("Resposta inesperada do CFM para " + uf + " na pagina " + pagina);
        }
        return resposta;
    }
}
