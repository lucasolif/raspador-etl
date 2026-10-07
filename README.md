# Trabalho 1 - dados públicos de médicos do CFM

Aplicação Java 17 com Spring Boot, Spring Batch, Selenium e MongoDB. Ao iniciar, ela abre a [Busca por Médicos do CFM](https://portal.cfm.org.br/busca-medicos/), consulta todos os resultados disponíveis com o filtro **MG** e depois com **PR**, persiste os registros em `medicos_brutos` e executa o ETL para a coleção `medicos`. As duas UFs atendem ao requisito mínimo do enunciado; MG aparece na figura de exemplo do trabalho.

## Como executar

1. Tenha Java 17 ou superior, Maven 3.9, Google Chrome e MongoDB em execução em `localhost:27017`.
2. Na IDE, execute a classe `MedicoApplication`, sem argumentos. Para a entrega, compile o projeto e execute o JAR `target/trabalho-01-cfm-1.0.0.jar`, também sem argumentos.
3. A aplicação confirma a conexão com o MongoDB, abre uma janela do Chrome e aplica os filtros de UF. Se o reCAPTCHA do portal apresentar um desafio, conclua-o nessa janela. A coleta continua automaticamente quando o portal liberar a consulta.
4. Ao terminar, a aplicação grava os documentos brutos e executa o job Spring Batch.

Na primeira execução, o Selenium Manager pode precisar baixar o driver compatível com o Chrome. A URI do MongoDB pode ser alterada pela variável de ambiente `MONGODB_URI`. Não é necessário digitar `--arquivo` ou `--processar`, nem copiar respostas JSON manualmente.

## Fluxo

```text
MedicoApplication
  -> MedicoCommandRunner
  -> CfmScrapingService: consulta MG e PR no navegador
  -> MedicoImportService: valida e persiste medicos_brutos
  -> MedicoBatchConfig: lê, transforma e grava medicos
```

O `CfmScrapingService` usa o formulário público do portal e captura as respostas JSON recebidas pela página após a verificação do reCAPTCHA. O projeto não tenta resolver nem contornar essa verificação. O portal entrega os resultados em páginas de até 10 registros; a aplicação percorre essas páginas automaticamente até alcançar o total informado pelo CFM. Cada página é importada diretamente para `medicos_brutos`, sem acumular todos os resultados em memória. O ETL só inicia após a coleta completa das duas UFs.

O `MedicoImportService` exige nome, CRM e UF coerente em cada registro. Preserva os campos de origem, exceto `SECURITYHASH`, que é um token temporário do portal. O ID `UF:CRM` permite atualizar o mesmo registro em execuções posteriores, sem criar duplicatas. Se uma das consultas não retornar registros válidos, o ETL não é iniciado.

O job lê `medicos_brutos` em ordem de ID, aplica `MedicoTransformService.transformar` e grava `medicos` em blocos de 100. O documento final possui `nome`, `crm`, `estado`, `especialidades` (descrição e RQE), `formacao` (instituição, campus e ano) e `situacao`, conforme o modelo do enunciado. O H2 em memória guarda apenas os metadados da execução do Spring Batch; os dados de médicos ficam no MongoDB.

## Relacao com o exemplo do professor

O projeto `resposta-refinado` do professor organiza o ETL em extrator, transformador, carregador e job. Aqui o mesmo fluxo aparece na etapa Spring Batch: `medicoReader` extrai da coleção bruta, `MedicoTransformService` transforma, o writer do `medicoStep` carrega a coleção final, e `medicoJob` coordena a etapa. A coleta no site foi acrescentada antes do ETL para que a aplicação obtenha os dados ao iniciar.

## Limites e dependencias externas

- O portal pode alterar seu formulário, seu endpoint, o formato JSON ou sua política de reCAPTCHA. Nesse caso, a coleta deve ser ajustada ao novo funcionamento público do site.
- A verificação humana eventualmente apresentada pelo reCAPTCHA depende de interação no navegador. A aplicação espera até três minutos pela resposta de cada página. Uma coleta completa pode levar bastante tempo, conforme o número de resultados e a resposta do portal.
- É necessário acesso à internet para consultar o CFM. Se o Selenium Manager ainda não tiver o driver em cache, também precisará de acesso à internet para obtê-lo.
- A transação dos metadados H2 não é distribuída com a gravação no MongoDB. Os IDs deterministas permitem repetir a carga sem multiplicar médicos.
