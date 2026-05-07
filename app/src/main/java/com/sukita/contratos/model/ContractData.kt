package com.sukita.contratos.model

/**
 * Dados variáveis de um contrato preenchido pelo usuário.
 * Não é persistido em banco — é montado na sessão e descartado
 * após a geração do PDF.
 */
data class ContractData(
    val apartment: Apartment,

    // Dados do locatário
    val tenantName: String,        // Ex: "Karina Da Costa Mendonca"
    val cpf: String,               // Ex: "078.440.702-93"
    val rg: String,                // Ex: "403380-9 SSP-AM"

    // Dados financeiros e prazo
    val rentValueCents: Long,      // Valor em centavos. Ex: 65000 = R$650,00
    val termMonths: Int,           // Prazo em meses. Ex: 12
    val startDate: String,         // DD/MM/YYYY
    val paymentDay: Int,           // Dia de pagamento (1-31)

    // Página final
    val signatureDate: String      // DD/MM/YYYY
) {
    /** Retorna o valor formatado: R$ 650,00 */
    fun formattedRentValue(): String {
        val reais = rentValueCents / 100
        val cents = rentValueCents % 100
        return "R\$ %,d,%02d".format(reais, cents)
            .replace(",", "X").replace(".", ",").replace("X", ".")
    }
}
