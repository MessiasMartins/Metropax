package br.com.sankhya.bhz.limitesLiberacao;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;

import java.math.BigDecimal;

public class AlteraNSU implements EventoProgramavelJava {


    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        processarEvento(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        processarEvento(event);
    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }

    private void processarEvento(PersistenceEvent event) throws Exception {
        DynamicVO vo = (DynamicVO) event.getVo();
        String tabela = vo.getValueObjectID();

        if ("TGFCAB".equalsIgnoreCase(tabela)) {
            validarCab(vo);
        } else if ("TGFFIN".equalsIgnoreCase(tabela)) {
            validarEAtualizarFin(vo);
        }
    }

    // --- BLOQUEIO E VALIDAÇÃO NO CABEÇALHO (TGFCAB) ---
    private void validarCab(DynamicVO cabVO) throws Exception {
        String statusNota = cabVO.asString("STATUSNOTA");

        String origem = cabVO.asString("ORIGEM");
        String provisao = cabVO.asString("PROVISAO");

        BigDecimal codTipTitCab = null;
        BigDecimal codTipVenda = cabVO.asBigDecimal("CODTIPVENDA");
        if (codTipVenda != null) {
            DynamicVO tpvVO = JapeFactory.dao("TipoVenda").findByPK(codTipVenda);
            if (tpvVO != null) {
                codTipTitCab = tpvVO.asBigDecimal("CODTIPTIT");
            }
        }

        if (codTipTitCab != null && "E".equalsIgnoreCase(origem) && "N".equalsIgnoreCase(provisao)) {
            DynamicVO titVO = JapeFactory.dao("TipoTitulo").findByPK(codTipTitCab);
            if (titVO != null) {
                String subTipoVenda = titVO.asString("SUBTIPOVENDA");
                // Bloqueia a confirmação se STATUSNOTA = L
                if ("L".equalsIgnoreCase(statusNota) && ("7".equals(subTipoVenda) || "8".equals(subTipoVenda))) {

                    // Consulta todos os lançamentos financeiros da nota
                    JapeWrapper finDAO = JapeFactory.dao("Financeiro");
                    DynamicVO[] fins = finDAO.find("NUNOTA = ?", cabVO.asBigDecimal("NUNOTA")).toArray(new DynamicVO[0]);

                    for (DynamicVO finVO : fins) {
                        BigDecimal codTipTitFin = finVO.asBigDecimal("CODTIPTIT");
                        if (codTipTitFin != null) {
                            DynamicVO titFinVO = JapeFactory.dao("TipoTitulo").findByPK(codTipTitFin);
                            if (titFinVO != null) {
                                String subTipoFin = titFinVO.asString("SUBTIPOVENDA");
                                if ("7".equals(subTipoFin) || "8".equals(subTipoFin)) {
                                    String nsu = finVO.asString("AD_NSU");
                                    String autPag = finVO.asString("AD_AUTPAG");
                                    if (nsu == null || nsu.trim().isEmpty() || autPag == null || autPag.trim().isEmpty()) {
                                        throw new MGEModelException(
                                                "Não é possível CONFIRMAR a nota: todos os títulos TEF (SUBTIPOVENDA 7 ou 8) devem ter NSU e Autorização preenchidos."
                                        );
                                    }
                                }
                            }
                        }
                    }

                    throw new MGEModelException(
                            "Não é permitido CONFIRMAR a nota com SUBTIPOVENDA 7 ou 8, origem 'E' e provisão 'N'."
                    );
                }
            }
        }
    }

    // --- ATUALIZAÇÃO DE TEF NO FINANCEIRO (TGFFIN) ---
    private void validarEAtualizarFin(DynamicVO finVO) throws Exception {
        BigDecimal codTipTit = finVO.asBigDecimal("CODTIPTIT");
        String origem = finVO.asString("ORIGEM");
        String provisao = finVO.asString("PROVISAO");

        if (codTipTit != null && "E".equalsIgnoreCase(origem) && "N".equalsIgnoreCase(provisao)) {
            JapeWrapper titDAO = JapeFactory.dao("TipoTitulo");
            DynamicVO titVO = titDAO.findByPK(codTipTit);
            if (titVO != null) {
                String subTipoVenda = titVO.asString("SUBTIPOVENDA");

                if (subTipoVenda != null && ("7".equals(subTipoVenda) || "8".equals(subTipoVenda))) {
                    String nsu = finVO.asString("AD_NSU");
                    String autPag = finVO.asString("AD_AUTPAG");

                    if (nsu == null || nsu.trim().isEmpty()) {
                        throw new MGEModelException(
                                "O campo NSU é obrigatório para títulos de Cartão de Crédito ou Débito."
                        );
                    }

                    if (autPag == null || autPag.trim().isEmpty()) {
                        throw new MGEModelException(
                                "O campo Autorização de Pagamento é obrigatório para títulos com SUBTIPOVENDA 7 ou 8 e ORIGEM 'E'."
                        );
                    }

                    // Atualiza ou cria TGFTEF
                    BigDecimal nuFin = finVO.asBigDecimal("NUFIN");
                    if (nuFin != null) {
                        JapeWrapper tefDAO = JapeFactory.dao("TEF");
                        DynamicVO tefVO = tefDAO.findOne("NUFIN = ?", nuFin);

                        if (tefVO != null) {
                            tefDAO.prepareToUpdate(tefVO)
                                    .set("NUMNSU", nsu)
                                    .set("AUTORIZACAO", autPag)
                                    .update();
                        } else {
                            tefDAO.create()
                                    .set("NUFIN", nuFin)
                                    .set("NUMNSU", nsu)
                                    .set("AUTORIZACAO", autPag)
                                    .save();
                        }
                    }
                }
            }
        }
    }
}