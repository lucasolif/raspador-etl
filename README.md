# Trabalho 01 — dados públicos de médicos do CFM

Aplicação Java 17, Spring Boot, Spring Batch e MongoDB baseada nos modelos e nas regras de transformação do projeto `resposta-refinado`. Os nomes de pacotes e pastas de código estão em inglês; variáveis e métodos estão em português. Classes de domínio usam o nome `Medico` e um sufixo técnico em inglês.

## O que o projeto faz

1. Importa respostas JSON de buscas públicas feitas por uma pessoa no [portal do CFM](https://portal.cfm.org.br/busca-medicos/), após a verificação humana do próprio portal.
2. Grava cada registro recebido na coleção MongoDB `medicos_brutos`, preservando os campos de origem, a UF, o CRM, o arquivo e a data da importação.
3. Confere se há dados de **ao menos duas UFs** antes de iniciar o ETL.
4. Executa um job Spring Batch que lê os registros brutos, normaliza os dados e grava a coleção `medicos`.

O projeto não inclui registros reais de médicos nem automatiza a verificação humana do portal. A coleta de cada página de resposta é feita no navegador; a importação e o ETL são executados pela aplicação. O portal pode mudar o formato de sua resposta. Confira os campos do arquivo antes de importar.

## Pré-requisitos

- Java 17 ou superior e Maven 3.9.
- MongoDB em `localhost:27017` ou uma URI definida na variável de ambiente `MONGODB_URI`.
- Duas ou mais respostas JSON de buscas por UFs diferentes. É possível importar várias páginas da mesma UF.

Os dados de médicos são armazenados **somente no MongoDB**. O arquivo local `batch-metadata.mv.db` é um banco H2 usado pelo Spring Batch para registrar execuções e etapas, e está no `.gitignore`.

## Como obter os arquivos de entrada

1. Acesse a busca pública do CFM, escolha uma UF e conclua a verificação exigida pelo portal.
2. No navegador, abra as ferramentas de desenvolvedor e a guia **Rede / Network**.
3. Faça a busca. Selecione a resposta da requisição `buscar_medicos`, copie o corpo JSON da guia **Resposta / Response** e salve em um arquivo `.json`, por exemplo `pr-pagina-1.json`.
4. Repita para outra UF, por exemplo `sc-pagina-1.json`. Guarde páginas adicionais em arquivos separados.

O formato esperado é uma lista em `dados`, como na resposta utilizada pela interface pública:

```json
{
  "status": "sucesso",
  "dados": [
    {
      "NM_MEDICO": "EXEMPLO FICTICIO",
      "NU_CRM_NATURAL": "12345",
      "SG_UF": "PR",
      "SITUACAO": "Regular",
      "ESPECIALIDADE": "CARDIOLOGIA - RQE Nº: 678",
      "NM_INSTITUICAO_GRADUACAO": "UNIVERSIDADE EXEMPLO - Campus Centro",
      "DT_GRADUACAO": "2008"
    }
  ]
}
```

**O registro acima é fictício e serve apenas para mostrar o formato.** Ao importar, a aplicação verifica nome, CRM e UF; um arquivo com UF divergente é recusado. O campo `SECURITYHASH`, quando presente, não é persistido porque não é necessário ao trabalho.

## Compilar e executar

Na pasta do projeto:

```powershell
mvn clean package
java -jar target/trabalho-01-cfm-1.0.0.jar "--arquivo=PR=C:\dados\pr-pagina-1.json" "--arquivo=SC=C:\dados\sc-pagina-1.json" --processar
```

Para importar páginas adicionais, acrescente outros argumentos `--arquivo=UF=caminho.json`. Para executar novamente o ETL sobre os registros já importados, use somente `--processar`. Para consultar o formato do comando, execute o JAR sem argumentos ou com `--ajuda`.

O ID `UF:CRM` permite que uma nova importação do mesmo médico atualize o documento existente, em vez de criar uma duplicata. O Spring Batch usa o mesmo ID na coleção normalizada. Para evitar o processamento de dados fictícios, não importe o JSON ilustrativo deste README.

## Modelo de dados

- `medicos_brutos`: `id`, `crm`, `estado`, `arquivoOrigem`, `dataImportacao`, `camposOriginais`.
- `medicos`: `id`, `nome`, `crm`, `estado`, `especialidades` (descrição e RQE), `formacao` (instituição, campus e ano) e `situacao`.

Os valores ausentes de instituição, campus, ano e RQE permanecem `null`; o projeto não substitui ausência por `0`. O campo de especialidades vazio resulta em uma lista vazia. A formatação exata depende do conteúdo exportado do CFM.

## Principais classes

| Classe | Responsabilidade |
|---|---|
| `MedicoCommandRunner` | Lê os argumentos, aciona a importação, verifica as UFs e inicia o job. |
| `MedicoImportService` | Valida e persiste o JSON público na coleção bruta. |
| `MedicoTransformService` | Converte os campos do CFM no modelo pedido pelo trabalho. |
| `MedicoBatchConfig` | Define reader, processor, writer, step e job do Spring Batch. |
| `MedicoRawRepository` / `MedicoRepository` | Acesso às duas coleções MongoDB. |

## Limites conhecidos

- A resposta da busca pública depende da interação humana com o portal. Não há acesso ao Webservice oficial de listagem neste projeto.
- Os registros exportados devem cobrir de fato pelo menos duas UFs; a aplicação valida isso antes do ETL, mas não faz as buscas por conta própria.
- O reader percorre a coleção bruta e o writer grava em blocos de 100. A transação do banco H2 de metadados não cria uma transação distribuída com o MongoDB. IDs determinísticos permitem repetir a carga sem multiplicar os médicos.
- Se o portal mudar nomes de campos ou separadores de especialidades, será necessário ajustar `MedicoImportService` e `MedicoTransformService`.
# raspador-etl
