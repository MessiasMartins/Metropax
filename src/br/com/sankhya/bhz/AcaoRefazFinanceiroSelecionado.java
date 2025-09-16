package br.com.sankhya.bhz;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;

public class AcaoRefazFinanceiroSelecionado implements AcaoRotinaJava {
    DynamicVO ultRafVO = null;

    public AcaoRefazFinanceiroSelecionado() {
    }

    public BigDecimal pegaUltimoDiaDoMes(Calendar calendar, Integer ano, Integer mes, Integer dia) {
        calendar.set(ano, mes, 1);
        return new BigDecimal(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    }

    public void doAction(ContextoAcao contextoAcao) throws Exception {
        JapeSession.SessionHandle hnd = null;
        hnd = JapeSession.open();
        String nuFinKey = "NUFIN";
        String codCtaBcoIntKey = "CODCTABCOINT";
        String codParcKey = "CODPARC";
        String codEmpKey = "CODEMP";

        try {
            JapeWrapper conDAO = JapeFactory.dao("Contrato");
            JapeWrapper finDAO = JapeFactory.dao("Financeiro");
            JapeWrapper ppgDAO = JapeFactory.dao("ParcelaPagamento");
            JapeWrapper marcaDAO = JapeFactory.dao("AD_MFIN");
            JapeWrapper rafDAO = JapeFactory.dao("RegistroAlteracaoFinanceiro");
            BigDecimal empresa = null;
            BigDecimal desconto = null;
            Registro[] linhas = contextoAcao.getLinhas();
            if (linhas.length < 1) {
                throw new Exception("Selecione pelo menos um registro.");
            }

            for (Registro linha : linhas) {
                BigDecimal valor = null;
                Integer diapagto = null;
                DynamicVO finVO = finDAO.findOne("NUFIN = ?", new Object[]{linha.getCampo(nuFinKey)});
                DynamicVO conVO = conDAO.findOne("NUMCONTRATO = ?", new Object[]{linha.getCampo("NUMCONTRATO")});
                this.ultRafVO = rafDAO.findOne("NUFIN = ? AND SEQUENCIA = (SELECT MAX(SEQUENCIA) FROM TGFRAF WHERE NUFIN = ?)", new Object[]{finVO.asBigDecimal(nuFinKey), finVO.asBigDecimal(nuFinKey)});
                valor = conVO.asBigDecimal("AD_VLRCONTRATO");
                BigDecimal codParc = conVO.asBigDecimal(codParcKey);
                empresa = conVO.asBigDecimal(codEmpKey);
                DynamicVO ppgVO = ppgDAO.findOne("CODEMP = ? AND CODTIPVENDA = ?", new Object[]{empresa, conVO.asBigDecimal("CODTIPVENDA")});
                if (ppgVO == null) {
                    throw new Exception("Erro DO-1: Nenhuma parcela encontrada para o Tipo de Negociação " + conVO.asBigDecimal("CODTIPVENDA") + ". Gentileza entrar em contato com o Financeiro");
                }

                if (ppgVO.asBigDecimal("CODTIPTITPAD") == null) {
                    throw new Exception("Erro DO-2:Nenhum tipo de título padrão configurado para a parcela do Tipo de Negociação informado no contrato. Gentileza entrar em contato com o Financeiro");
                }

                BigDecimal codTipTit = ppgVO.asBigDecimal("CODTIPTITPAD");
                if (codTipTit.equals(new BigDecimal(53))) {
                    desconto = new BigDecimal(5);
                } else {
                    desconto = BigDecimal.ZERO;
                }

                BigDecimal codBco = ppgVO.asBigDecimal("CODBCOPAD");
                BigDecimal conta = ppgVO.asBigDecimal(codCtaBcoIntKey);
                diapagto = conVO.asInt("DIAPAG");
                Timestamp dtvencimento = finVO.asTimestamp("DTVENC");
                Calendar dtvenc = Calendar.getInstance();
                dtvenc.setTime(dtvencimento);
                int mes = dtvenc.get(Calendar.MONTH);
                int ano = dtvenc.get(Calendar.YEAR);
                Calendar dt = Calendar.getInstance();
                BigDecimal ultimodiadomes = this.pegaUltimoDiaDoMes(dtvenc, ano, mes, diapagto);
                if (mes == 1 && diapagto > 28) {
                    diapagto = 28;
                    mes = 1;
                } else if (ultimodiadomes.compareTo(new BigDecimal(diapagto)) < 0) {
                    diapagto = ultimodiadomes.intValue();
                }

                dt.set(ano, mes, diapagto);
                final BigDecimal valorFinal = (new BigDecimal(String.valueOf(valor))).setScale(4, RoundingMode.HALF_EVEN);
                String adesao = finVO.asString("AD_ADESAO");
                if (adesao == null) {
                    adesao = "B";
                }

                if (this.ultRafVO != null) {
                    rafDAO.deleteByCriteria(
                            "NUFIN = ? AND SEQUENCIA = (SELECT MAX(SEQUENCIA) FROM TGFRAF WHERE NUFIN = ?)",
                            new Object[]{finVO.asBigDecimal(nuFinKey), finVO.asBigDecimal(nuFinKey)});
                }

                if (adesao.contains("S")) {
                    finDAO.prepareToUpdate(finVO).set(codEmpKey, empresa)
                            .set("ORIGEM", "F")
                            .set(codParcKey, codParc)
                            .set("CODTIPTIT", codTipTit)
                            .set("CODBCO", codBco)
                            .set(codCtaBcoIntKey, conta)
                            .set("CODIGOBARRA", (Object) null)
                            .set("LINHADIGITAVEL", (Object) null)
                            .set("MONIOCOREM", "N")
                            .set("NOSSONUM", (Object) null)
                            .set("EMVPIX", (Object) null)
                            .update();
                } else {
                    finDAO.prepareToUpdate(finVO).set(codEmpKey, empresa)
                            .set("ORIGEM", "F")
                            .set("VLRDESDOB", valorFinal)
                            .set("VLRDESC", desconto)
                            .set(codParcKey, codParc)
                            .set("DTVENC", new Timestamp(dt.getTimeInMillis()))
                            .set("CODTIPTIT", codTipTit)
                            .set("CODBCO", codBco)
                            .set(codCtaBcoIntKey, conta)
                            .set("CODIGOBARRA", (Object) null)
                            .set("LINHADIGITAVEL", (Object) null)
                            .set("NUMREMESSA", (Object) null)
                            .set("MONIOCOREM", "N")
                            .set("NOSSONUM", (Object) null)
                            .set("EMVPIX", (Object) null)
                            .update();
                }

                EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
                JdbcWrapper jdbc = dwfFacade.getJdbcWrapper();
                jdbc.openSession();
                try {
                    // =====================================================
                    // BLOCO NOVO - ControleTituloFinanceiro / HBA
                    // =====================================================
                    JapeWrapper contaDAO = JapeFactory.dao("ContaBancaria");
                    DynamicVO oldConta = contaDAO.findOne("CODCTABCOINT  = ?",
                            new Object[]{finVO.asBigDecimal(codCtaBcoIntKey)});

                    if (oldConta != null
                            && oldConta.asBigDecimal("IDAPIBANCO") != null
                            && "S".equals(oldConta.asString("STATUSAPI"))) {

                        PreparedStatement psHba = jdbc.getConnection().prepareStatement(
                                "SELECT * FROM TGFHBA WHERE ID = (SELECT MAX(ID) FROM TGFHBA WHERE NUFIN = ?)");
                        psHba.setBigDecimal(1, finVO.asBigDecimal("NUFIN"));
                        ResultSet rsHba = psHba.executeQuery();

                        if (rsHba.next()) {
                            if (!"X".equals(rsHba.getString("TIPOENVIO"))) {
                                String statusBoletoHba = rsHba.getString("STATUSBANCO");

                                if (statusBoletoHba == null) {
                                    JapeWrapper hbaDAO = JapeFactory.dao("ControleTituloFinanceiro");
                                    ((FluidUpdateVO) hbaDAO
                                            .prepareToUpdateByPK(new Object[]{rsHba.getString("ID")})
                                            .set("TIPOENVIO", "X")).update();

                                } else {
                                    DynamicVO hbaVO = (DynamicVO) dwfFacade
                                            .getDefaultValueObjectInstance("ControleTituloFinanceiro");

                                    hbaVO.setProperty("NUFIN", finVO.asBigDecimal("NUFIN"));
                                    hbaVO.setProperty("NOSSONUM", rsHba.getString("NOSSONUM"));
                                    hbaVO.setProperty("STATUS", "A");
                                    hbaVO.setProperty("STATUSENVIO", "A");
                                    hbaVO.setProperty("TIPOENVIO", "X");
                                    hbaVO.setProperty("DHALTER", TimeUtils.getNow());
                                    hbaVO.setProperty("DHENVIOAPI", (Object) null);
                                    hbaVO.setProperty("STATUSBANCO", (Object) null);
                                    hbaVO.setProperty("MSGERRO", (Object) null);
                                    hbaVO.setProperty("IDAPIBANCO", oldConta.asBigDecimal("IDAPIBANCO"));

                                    dwfFacade.createEntity("ControleTituloFinanceiro", (EntityVO) hbaVO);
                                }
                            }

                            // Apaga ocorrências antigas
                            rafDAO.deleteByCriteria("NUFIN = ?",
                                    new Object[]{finVO.asBigDecimal("NUFIN")});

                            // Recria ocorrência se houver config
                            JapeWrapper alteracaoFinanceiroDAO = JapeFactory.dao("ConfRegAlteracaoFinanceiro");
                            if (finVO.asBigDecimal("CODCTABCOINT") != null) {
                                Collection<DynamicVO> confs = alteracaoFinanceiroDAO
                                        .find("CODCTABCOINT  = ?", new Object[]{finVO.asBigDecimal("CODCTABCOINT")});
                                if (confs != null && !confs.isEmpty()) {
                                    DynamicVO rafVO = (DynamicVO) dwfFacade
                                            .getDefaultValueObjectInstance("RegistroAlteracaoFinanceiro");
                                    rafVO.setProperty("NUFIN", finVO.asBigDecimal("NUFIN"));
                                    rafVO.setProperty("SEQUENCIA", new BigDecimal(1));
                                    rafVO.setProperty("NUREMESSA", (Object) null);
                                    rafVO.setProperty("CODUSU", AuthenticationInfo.getCurrent().getUserID());
                                    rafVO.setProperty("DTALTER", TimeUtils.getNow());
                                    rafVO.setProperty("CAMPO", " ");
                                    rafVO.setProperty("STATUS", "A");
                                    rafVO.setProperty("OCORRENCIA", "01");
                                    rafVO.setProperty("TIPO", "E");
                                    rafVO.setProperty("TIPOENVIO", "B");
                                    rafVO.setProperty("VALORENVIADO", (Object) null);

                                    dwfFacade.createEntity("RegistroAlteracaoFinanceiro", (EntityVO) rafVO);

                                    // Zera NOSSONUM no TGFFIN
                                    PreparedStatement psUpdate = jdbc.getConnection()
                                            .prepareStatement("UPDATE TGFFIN SET NOSSONUM=null WHERE NUFIN=?");
                                    psUpdate.setBigDecimal(1, finVO.asBigDecimal("NUFIN"));
                                    psUpdate.executeUpdate();
                                    psUpdate.close();
                                }
                            }
                        }
                        rsHba.close();
                        psHba.close();
                    }
                    // =====================================================

                    // Continua fluxo normal: grava RAF + marca
                    String sql = "SELECT ISNULL(MAX(SEQUENCIA),0)+1 AS PROXSEQ FROM TGFRAF WHERE NUFIN = ?";
                    BigDecimal proxSeq = BigDecimal.ONE;

                    try (PreparedStatement ps = jdbc.getConnection().prepareStatement(sql)) {
                        ps.setBigDecimal(1, finVO.asBigDecimal("NUFIN"));
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                proxSeq = rs.getBigDecimal("PROXSEQ");
                            }
                        }
                    }

                    rafDAO.create()
                            .set("CAMPO", " ")
                            .set("CODUSU", AuthenticationInfo.getCurrent().getUserID())
                            .set("DTALTER", TimeUtils.getNow())
                            .set("NUFIN", finVO.asBigDecimal(nuFinKey))
                            .set("NUREMESSA", null)
                            .set("OCORRENCIA", "01")
                            .set("SEQUENCIA", proxSeq)
                            .set("STATUS", "A")
                            .set("TIPO", "E")
                            .set("TIPOENVIO", "B")
                            .set("VALORENVIADO", null)
                            .save();

                    marcaDAO.create().set("NUFIN", finVO.asBigDecimal(nuFinKey))
                            .set("TIPO", "R")
                            .set("CODUSU", AuthenticationInfo.getCurrent().getUserID())
                            .set("DTINSERT", TimeUtils.getNow())
                            .save();
                } finally {
                    jdbc.closeSession();
                }
            }

            contextoAcao.setMensagemRetorno("Financeiro Atualizado!!!\nBoletos atualizados!!!");
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        } finally {
            JapeSession.close(hnd);
        }
    }
}
