package br.com.sankhya.bhz;

import br.com.sankhya.bh.Utils.ChecaPromocao;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AcaoFaturaContrato implements AcaoRotinaJava {
    JapeWrapper conDAO = JapeFactory.dao("Contrato");
    JapeWrapper ppgDAO = JapeFactory.dao("ParcelaPagamento");
    Timestamp refFat = null;
    int diaPagamento = 0;

    public void doAction(ContextoAcao contextoAcao) throws Exception {
        try {
            Registro[] linhas = contextoAcao.getLinhas();
            validarLinhas(linhas);
            this.refFat = obterTimestampReferencia(contextoAcao);

            for (Registro linha : linhas) {
                DynamicVO conVO = this.conDAO.findByPK(new Object[]{linha.getCampo("NUMCONTRATO")});
                BigDecimal codPlan = conVO.asBigDecimal("AD_CODPLAN");
                BigDecimal parcelas = getParcelas(codPlan);
                BigDecimal valor = getValorContrato(conVO.asBigDecimal("NUMCONTRATO"));
                processarFaturas(parcelas, conVO, valor);
                atualizarReferenciaProximaFatura(parcelas, conVO);
            }

            contextoAcao.setMensagemRetorno("Financeiro dos contratos gerados com sucesso!!!");
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        }
    }

    private void validarLinhas(Registro[] linhas) throws Exception {
        if (linhas.length < 1) {
            throw new Exception("Selecione 1 ou mais registros");
        }
    }

    private BigDecimal getParcelas(BigDecimal codPlan) throws Exception {
        JapeWrapper plaDAO = JapeFactory.dao("AD_CADPLAFUN");
        DynamicVO plaVO = plaDAO.findByPK(new Object[]{codPlan});
        return plaVO.asBigDecimal("QTDPARCELA");
    }

    private BigDecimal getValorContrato(BigDecimal numContrato) throws Exception {
        JapeWrapper preDAO = JapeFactory.dao("PrecoContrato");
        return preDAO.findOne(
                "NUMCONTRATO = ? AND REFERENCIA = (SELECT MAX(REFERENCIA) FROM TCSPRE WHERE NUMCONTRATO = ?)",
                new Object[]{numContrato, numContrato}
        ).asBigDecimal("VALOR");
    }

    private void ajustarParaUltimoDiaDoMes(Calendar calVenc, int diaPagamento) {
        int ultimoDiaDoMes = calVenc.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (diaPagamento > ultimoDiaDoMes) {
            calVenc.set(Calendar.DAY_OF_MONTH, ultimoDiaDoMes);
        }
    }

    private BigDecimal pegaUltimoDiaDoMes(Calendar calendar, Integer ano, Integer mes) {
        calendar.set(ano, mes, 1);
        return new BigDecimal(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    }

    private void proximoVencimento(Calendar calVenc, int mesSomar) {
        int mes = calVenc.get(Calendar.MONTH) + mesSomar;
        int ano = calVenc.get(Calendar.YEAR);
        int ultimoDiaDoMes = pegaUltimoDiaDoMes(calVenc, ano, mes).intValue();

        if (calVenc.get(Calendar.MONTH) == 12) {
            ano++;
            mes = 0;
            calVenc.set(ano, mes, this.diaPagamento);
        } else if (ultimoDiaDoMes < this.diaPagamento) {
            calVenc.set(ano, mes, ultimoDiaDoMes);
        } else {
            calVenc.set(ano, mes, this.diaPagamento);
        }
    }

    private void processarFaturas(BigDecimal parcelas, DynamicVO conVO, BigDecimal valor) throws Exception {
        Calendar calVenc = Calendar.getInstance();
        calVenc.setTime(this.refFat);
        this.diaPagamento = conVO.asInt("DIAPAG");

        if (calVenc.get(Calendar.DAY_OF_MONTH) > this.diaPagamento) {
            calVenc.add(Calendar.MONTH, 1);
        }

        calVenc.set(Calendar.DAY_OF_MONTH, this.diaPagamento);

        BigDecimal codEmpresa = conVO.asBigDecimal("CODEMP");
        DynamicVO ppgVO = this.ppgDAO.findOne(
                "CODEMP = ? AND CODTIPVENDA = ?",
                new Object[]{codEmpresa, conVO.asBigDecimal("CODTIPVENDA")}
        );

        BigDecimal codTipTit = ppgVO.asBigDecimal("CODTIPTITPAD");
        BigDecimal codBco = ppgVO.asBigDecimal("CODBCOPAD");
        BigDecimal codCtaBco = ppgVO.asBigDecimal("CODCTABCOINT");

        for (BigDecimal contador = BigDecimal.ZERO;
             contador.compareTo(parcelas) < 0;
             contador = contador.add(BigDecimal.ONE)) {

            ajustarParaUltimoDiaDoMes(calVenc, this.diaPagamento);
            Timestamp dtvenc = new Timestamp(calVenc.getTimeInMillis());
            validarContrato(conVO);

            BigDecimal desconto = calcularDesconto(conVO);
            BigDecimal valorFinal = calcularValorFinal(conVO, valor, contador);

            criarFinanceiro(conVO, valorFinal, desconto, dtvenc, codTipTit, codBco, codCtaBco);
            marcarFinanceiroGerado();
            proximoVencimento(calVenc, 1);
        }
    }

    private Timestamp obterTimestampReferencia(ContextoAcao contextoAcao) throws Exception {
        String refFat = contextoAcao.getParam("REFFAT").toString();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = formatter.parse(refFat);
        Timestamp ts = new Timestamp(date.getTime());

        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0);
        hoje.set(Calendar.MINUTE, 0);
        hoje.set(Calendar.SECOND, 0);
        hoje.set(Calendar.MILLISECOND, 0);

        if (ts.before(hoje.getTime())) {
            throw new Exception("Não é permitido inserir datas retroativas, insira uma data valida por gentileza!");
        }
        return ts;
    }

    private void validarContrato(DynamicVO conVO) throws Exception {
        if (conVO.asBigDecimalOrZero("CODTIPVENDA").compareTo(BigDecimal.ZERO) == 0) {
            throw new Exception("Cadastro do Contrato sem o Tipo de Negociação. Verificar!");
        }
    }

    private BigDecimal calcularDesconto(DynamicVO conVO) throws Exception {
        JapeWrapper ppgDAO = JapeFactory.dao("ParcelaPagamento");
        DynamicVO ppgVO = ppgDAO.findOne(
                "CODEMP = ? AND CODTIPVENDA = ?",
                new Object[]{conVO.asBigDecimal("CODEMP"), conVO.asBigDecimal("CODTIPVENDA")}
        );
        return ppgVO.asBigDecimal("CODTIPTITPAD").equals(BigDecimal.valueOf(53L))
                ? BigDecimal.valueOf(5L)
                : BigDecimal.ZERO;
    }

    private BigDecimal calcularValorFinal(DynamicVO conVO, BigDecimal valor, BigDecimal contador) throws Exception {
        return AuthenticationInfo.getCurrent().getUserID().compareTo(BigDecimal.valueOf(7L)) == 0
                ? ChecaPromocao.checaPromocao(conVO.asBigDecimal("AD_CODPLAN"), "p_mensalidade", valor, contador)
                : valor;
    }

    private void criarFinanceiro(DynamicVO conVO, BigDecimal valorFinal, BigDecimal desconto,
                                 Timestamp dtvenc, BigDecimal codTipTit, BigDecimal codBco, BigDecimal codCtaBco) throws Exception {
        JapeWrapper finDAO = JapeFactory.dao("Financeiro");
        finDAO.create()
                .set("CODEMP", conVO.asBigDecimal("CODEMP"))
                .set("NUMNOTA", BigDecimal.ZERO)
                .set("DTNEG", TimeUtils.getNow())
                .set("DHMOV", TimeUtils.getNow())
                .set("DTVENC", dtvenc)
                .set("CODPARC", conVO.asBigDecimal("CODPARC"))
                .set("CODNAT", BigDecimal.valueOf(1010100L))
                .set("VLRDESDOB", valorFinal)
                .set("VLRDESC", desconto)
                .set("RECDESP", BigDecimal.ONE)
                .set("CODTIPTIT", codTipTit)
                .set("ORIGEM", "F")
                .set("NUMCONTRATO", conVO.asBigDecimal("NUMCONTRATO"))
                .set("CODBCO", codBco)
                .set("CODCTABCOINT", codCtaBco)
                .save();
    }

    private void marcarFinanceiroGerado() throws Exception {
        JapeWrapper marcaDAO = JapeFactory.dao("AD_MFIN");
        marcaDAO.create()
                .set("NUFIN", BigDecimal.ZERO)
                .set("TIPO", "F")
                .set("CODUSU", AuthenticationInfo.getCurrent().getUserID())
                .set("DTINSERT", TimeUtils.getNow())
                .save();
    }

    private void atualizarReferenciaProximaFatura(BigDecimal parcelas, DynamicVO conVO) throws Exception {
        Calendar calRef = Calendar.getInstance();
        Timestamp refProxFat = conVO.asTimestamp("DTREFPROXFAT");

        if (refProxFat != null) {
            calRef.setTime(refProxFat);
        } else {
            calRef.setTime(new Date());
        }

        calRef.add(Calendar.MONTH, parcelas.intValue());

        conDAO.prepareToUpdate(conVO)
                .set("DTREFPROXFAT", new Timestamp(calRef.getTimeInMillis()))
                .update();
    }
}
