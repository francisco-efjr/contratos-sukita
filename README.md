# contratos-sukita

Aplicativo Android local para geração de contratos de locação de imóvel em PDF.

---

## O que o app faz

1. **Seleciona o imóvel** (Apt. 19 — Boas Novas, e outros cadastrados)
2. **Lê o documento do inquilino** com a câmera ou galeria (OCR local via ML Kit — nenhum dado sai do celular)
3. **Preenche os dados do contrato**: nome, CPF, RG, valor, prazo, data de início, dia de pagamento
4. **Calcula automaticamente**: data de término, valor por extenso (ex: *SEISCENTOS E CINQUENTA REAIS*)
5. **Gera o PDF final** com o contrato completo
6. **Compartilha ou salva** o PDF (WhatsApp, e-mail, Downloads)

---

## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitetura | MVVM |
| Banco local | Room (SQLite) |
| OCR | ML Kit Text Recognition (offline) |
| Câmera | CameraX |
| PDF | WebView + PrintedPdfDocument (A4) |
| Templates | HTML com placeholders |

---

## Como configurar e compilar

### Pré-requisitos
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17+
- Dispositivo/emulador com Android 7.0+ (API 24)

### Passos
```bash
# Clone o repositório
git clone https://github.com/francisco-efjr/contratos-sukita.git
cd contratos-sukita

# Abra no Android Studio
# File → Open → selecione a pasta contratos-sukita
# Aguarde o Gradle sync
# Run 'app' (▶)
```

> O Android Studio gera automaticamente o Gradle Wrapper na primeira abertura.
> Não é necessário instalar o Gradle manualmente.

---

## Como adicionar um novo imóvel

Edite o arquivo:
```
app/src/main/java/com/sukita/contratos/data/AppDatabase.kt
```

Adicione uma entrada na função `initialApartments()`:
```kotlin
Apartment(
    name = "Apt. 25 — Conjunto ABC",
    address = "Rua X, Conjunto ABC, Nº 10, Bairro Y, Manaus-AM",
    ucCode = "1234567-8 C-01",
    conjunto = "ABC",
    templateHtmlAsset = "templates/contract_base.html"
)
```

> Se o novo imóvel tiver cláusulas diferentes, crie um novo arquivo HTML em
> `app/src/main/assets/templates/` e aponte `templateHtmlAsset` para ele.

---

## Estrutura dos placeholders do template HTML

| Placeholder | Exemplo preenchido |
|---|---|
| `{{TENANT_NAME}}` | Karina da Costa Mendonca |
| `{{TENANT_NAME_UPPER}}` | KARINA DA COSTA MENDONCA |
| `{{CPF}}` | 078.440.702-93 |
| `{{RG}}` | 403380-9 SSP-AM |
| `{{PROPERTY_ADDRESS}}` | Rua 01, Conj. Boas Novas, Nº 20... |
| `{{UC_CODE}}` | 2377157-7 C-21 |
| `{{RENT_VALUE}}` | 650,00 |
| `{{RENT_VALUE_WORDS}}` | SEISCENTOS E CINQUENTA REAIS |
| `{{TERM_MONTHS}}` | 12 |
| `{{TERM_MONTHS_WORDS}}` | DOZE MESES |
| `{{START_DATE}}` | 07/05/2026 |
| `{{END_DATE}}` | 07/05/2027 |
| `{{PAYMENT_DAY}}` | 07 |
| `{{SIGNATURE_DATE}}` | 07 de Maio de 2026 |

---

## Leitura de documento (OCR)

- Funciona **100% offline** — nenhuma imagem é enviada para servidores externos
- Suporta CNH, RG, Passaporte e outros documentos com texto impresso
- A precisão depende da qualidade da foto (iluminação, foco, ângulo)
- **Sempre revise** os campos extraídos antes de confirmar
- Documentos com hologramas, plástico reflexivo ou texto manuscrito podem ter leitura parcial

---

## Assinatura Digital gov.br — Nota de Viabilidade

### Resumo
A integração direta com a plataforma de assinatura digital do gov.br **não é tecnicamente viável** para apps Android de terceiros no momento.

### Detalhes

| Item | Status |
|---|---|
| API pública oficial para terceiros | Não existe |
| SDK Android oficial para assinatura | Não disponível |
| Scraping/automação não oficial | Proibido e inseguro |
| Fluxo de assinatura via app gov.br | Disponível, mas manual |

### Alternativa recomendada
1. O app gera o PDF do contrato
2. O PDF é compartilhado por WhatsApp/e-mail com o inquilino
3. O inquilino acessa **assinador.iti.br** (portal oficial do gov.br para assinaturas digitais)
4. Ele faz upload do PDF e assina digitalmente com o certificado gov.br
5. O arquivo assinado (com validade jurídica) é devolvido ao locador

Essa alternativa está em conformidade com a Lei nº 14.063/2020 e não requer nenhuma integração técnica adicional no app.

---

## Segurança e privacidade

- Nenhum dado pessoal (CPF, RG, nome) é enviado para servidores externos
- O OCR roda localmente via ML Kit
- O PDF gerado fica no cache interno do app até ser compartilhado ou salvo pelo usuário
- Imagens capturadas para OCR não são armazenadas permanentemente

---

## Testes unitários

Execute via Android Studio ou terminal:
```bash
./gradlew test
```

Coberturas:
- Formatação e validação de CPF
- Conversão de valores em extenso (PT-BR)
- Cálculo de data de término
- Formatação de moeda
- Formatação de nomes

---

## Licença

Uso privado. Todos os direitos reservados.
