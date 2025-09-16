//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package br.com.sankhya.bhz.utils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.dao.PersistentObjectUID;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.LiberacaoSolicitada;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import com.google.gdata.util.common.base.Nullable;
import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilitarios {
    public String processName = null;

    public Utilitarios() throws IOException {
    }

    public static BigDecimal getAtualEstoque(DynamicVO cabVO) {
        switch (cabVO.asString("TipoOperacao.ATUALEST")) {
            case "B":
                return BigDecimal.valueOf(-1L);
            case "E":
                return BigDecimal.valueOf(1L);
            default:
                return BigDecimal.valueOf(0L);
        }
    }

    public static void adicionarFilaEmail(String assunto, String mensagem, String destinatarios) throws Exception {
        JapeWrapper emailDAO = JapeFactory.dao("MSDFilaMensagem");
        Timestamp dtAgora = new Timestamp(System.currentTimeMillis());
        String[] emails = destinatarios.split(";");
        BigDecimal smtp = (BigDecimal)MGECoreParameter.getParameter("BH_CONSMTPPP");

        for(String email : emails) {
            FluidCreateVO creEmail = emailDAO.create();
            creEmail.set("CODCON", BigDecimal.ZERO);
            creEmail.set("CODSMTP", smtp);
            creEmail.set("EMAIL", email);
            creEmail.set("ASSUNTO", assunto);
            creEmail.set("MENSAGEM", mensagem.toCharArray());
            creEmail.set("DTENTRADA", dtAgora);
            creEmail.set("STATUS", "Pendente");
            creEmail.set("TIPOENVIO", "E");
            creEmail.set("MAXTENTENVIO", BigDecimalUtil.valueOf(3L));
            creEmail.save();
        }

    }

    public static Boolean emailValido(String email) {
        boolean isEmailIdValid = false;
        if (email != null && email.length() > 0 && email.contains("@")) {
            String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
            Pattern pattern = Pattern.compile(expression, 2);
            Matcher matcher = pattern.matcher(email);
            if (matcher.matches()) {
                isEmailIdValid = true;
            }
        }

        return isEmailIdValid;
    }

    public static Object isNull(Object valor, Object seNulo) {
        return valor == null ? seNulo : valor;
    }

    public static void confirmarNota(BigDecimal nuNota) throws Exception {
        String toResult = "";
        CACHelper cacHelper = new CACHelper();
        BarramentoRegra barramento = BarramentoRegra.build(CACHelper.class, "regrasConfirmacaoCAC.xml", AuthenticationInfo.getCurrent());
        cacHelper.confirmarNota(nuNota, barramento, false);
        if (barramento.getLiberacoesSolicitadas().size() == 0 && barramento.getErros().size() == 0) {
            System.out.println("Nota Confirmada " + nuNota + "");
        } else {
            if (barramento.getErros().size() > 0) {
                System.out.println("Erro na confirmao " + nuNota);
                Iterator var4 = barramento.getErros().iterator();
                if (var4.hasNext()) {
                    Exception e = (Exception)var4.next();
                    toResult = e.getMessage();
                }
            }

            if (barramento.getLiberacoesSolicitadas().size() > 0) {
                System.out.println("Erro na confirmao " + nuNota + ". Foi solicitada liberaes");
                toResult = "Liberaes solicitadas - \n";
                Iterator var6 = barramento.getLiberacoesSolicitadas().iterator();
                if (var6.hasNext()) {
                    LiberacaoSolicitada e = (LiberacaoSolicitada)var6.next();
                    toResult = toResult + "Evento: " + e.getEvento() + (e.getDescricao() != null ? " Descrio:  " + e.getDescricao() + "\n" : "\n");
                }
            }
        }

        System.out.println(toResult);
    }

    public static DynamicVO duplicaRegistroVO(DynamicVO voOrigem, String entidade) throws Exception {
        return duplicaRegistroVO(voOrigem, entidade, (Map)null);
    }

    public static DynamicVO duplicaRegistroVO(DynamicVO voOrigem, String entidade, Map<String, Object> map) throws Exception {
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        EntityDAO rootDAO = dwfFacade.getDAOInstance(entidade);
        DynamicVO destinoVO = voOrigem.buildClone();
        limparPk(destinoVO, rootDAO);
        if (map != null) {
            for(String campo : map.keySet()) {
                destinoVO.setProperty(campo, map.get(campo));
            }
        }

        PersistentLocalEntity createEntity = dwfFacade.createEntity(entidade, (EntityVO)destinoVO);
        DynamicVO save = (DynamicVO)createEntity.getValueObject();
        return save;
    }

    private static void limparPk(DynamicVO vo, EntityDAO rootDAO) throws Exception {
        PersistentObjectUID objectUID = rootDAO.getSQLProvider().getPkObjectUID();
        EntityPropertyDescriptor[] pkFields = objectUID.getFieldDescriptors();

        for(EntityPropertyDescriptor pkField : pkFields) {
            vo.setProperty(pkField.getField().getName(), (Object)null);
        }

    }

    public static void recalculaImpostosNota(BigDecimal nuNota) throws Exception {
        ImpostosHelpper impostohelp = new ImpostosHelpper();
        impostohelp.setForcarRecalculo(true);
        impostohelp.setSankhya(false);
        impostohelp.calcularImpostos(nuNota);
    }

    public static String pegaBDname() throws Exception {
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);
        String dbName = null;

        try {
            sql.appendSql("SELECT DB_NAME() AS NAME");
            ResultSet resultSet = sql.executeQuery();
            if (resultSet.next()) {
                dbName = resultSet.getString("NAME");
            }
        } catch (Exception var5) {
            dbName = "desconhecido";
        }

        return dbName;
    }

    public static Timestamp addDays(Timestamp date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(5, days);
        return new Timestamp(cal.getTime().getTime());
    }

    public static Timestamp convertToTimestamp(String dateTime) throws Exception {
        Timestamp retorno = null;
        if (dateTime != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Timestamp dtNeg = new Timestamp(df.parse(dateTime).getTime());
            Calendar dtNegF = Calendar.getInstance();
            dtNegF.setTime(dtNeg);
            retorno = new Timestamp(dtNegF.getTimeInMillis());
        }

        return retorno;
    }

    public static void tempoExecucao(long startTime, Timestamp dhinicio, String classe, String sucesso) throws Exception {
        JapeSession.SessionHandle hnd = null;
        long stopTime = System.currentTimeMillis();
        long time = stopTime - startTime;

        try {
            hnd = JapeSession.open();
            ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)JapeFactory.dao("AD_TEMPEXPERS").create().set("DHINICIO", dhinicio)).set("CLASSE", classe)).set("INICIO", new BigDecimal(startTime))).set("FIM", new BigDecimal(stopTime))).set("TEMPO", new BigDecimal(time))).set("SUCESSO", sucesso)).save();
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        } finally {
            JapeSession.close(hnd);
        }

    }

    public static void confirmarNota2(BigDecimal nuNota, AuthenticationInfo aut_info) throws Exception {
        String toResult = "";
        CACHelper cacHelper = new CACHelper();
        BarramentoRegra barramento = BarramentoRegra.build(CACHelper.class, "regrasConfirmacaoCAC.xml", aut_info);
        cacHelper.confirmarNota(nuNota, barramento, false);
        if (barramento.getLiberacoesSolicitadas().size() == 0 && barramento.getErros().size() == 0) {
            System.out.println("Nota Confirmada " + nuNota + "");
        } else {
            if (barramento.getErros().size() > 0) {
                System.out.println("Erro na confirmao " + nuNota);
                Iterator var5 = barramento.getErros().iterator();
                if (var5.hasNext()) {
                    Exception e = (Exception)var5.next();
                    toResult = e.getMessage();
                }
            }

            if (barramento.getLiberacoesSolicitadas().size() > 0) {
                System.out.println("Erro na confirmao " + nuNota + ". Foi solicitada liberaes");
                toResult = "Liberaes solicitadas - \n";
                Iterator var7 = barramento.getLiberacoesSolicitadas().iterator();
                if (var7.hasNext()) {
                    LiberacaoSolicitada e = (LiberacaoSolicitada)var7.next();
                    toResult = toResult + "Evento: " + e.getEvento() + (e.getDescricao() != null ? " Descrio:  " + e.getDescricao() + "\n" : "\n");
                }
            }
        }

        System.out.println(toResult);
    }

    public static void enviar_email() {
        try {
            String conteudoTXT = "CONTEÚDO TESTE";
            String assuntoTXT = "ASSUNTO TESTE";
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            DynamicVO filaVO = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
            filaVO.setProperty("EMAIL", "victor@metropax.com.br");
            filaVO.setProperty("CODCON", BigDecimal.ZERO);
            filaVO.setProperty("CODMSG", (Object)null);
            filaVO.setProperty("STATUS", "Pendente");
            filaVO.setProperty("TIPOENVIO", "E");
            filaVO.setProperty("TIPODOC", "N");
            filaVO.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3L));
            filaVO.setProperty("CODSMTP", BigDecimal.ONE);
            filaVO.setProperty("ASSUNTO", assuntoTXT);
            filaVO.setProperty("MENSAGEM", conteudoTXT.toCharArray());
            boolean useReplyTo = (Boolean)MGECoreParameter.getParameter("mge.responder.email.usuario.logado");
            filaVO.setProperty("CODUSUREMET", BigDecimal.ZERO);
            PersistentLocalEntity filaEntity = dwfFacade.createEntity("MSDFilaMensagem", (EntityVO)filaVO);
            filaVO = (DynamicVO)filaEntity.getValueObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Date convertTimestampToDate(Timestamp timestamp) {
        Date date = new Date(timestamp.getTime());
        return date;
    }

    public static int calcMonthsBeweenTwoDates(Date startDate, Date endDate) {
        int months = 0;
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);
        int startYear = startCalendar.get(1);
        int endYear = endCalendar.get(1);
        int startMonth = startCalendar.get(2);
        int endMonth = endCalendar.get(2);
        int startDay = startCalendar.get(5);
        int endDay = endCalendar.get(5);
        if (startYear == endYear) {
            months = endMonth - startMonth;
            if (endDay < startDay) {
                --months;
            }
        } else {
            months = 12 - startMonth + endMonth;
            if (endDay < startDay) {
                --months;
            }

            for(int i = startYear + 1; i < endYear; ++i) {
                months += 12;
            }
        }

        return months;
    }

    public static Timestamp addMonthstoDate(@Nullable Timestamp date, int months) {
        Calendar cal = Calendar.getInstance();
        if (date == null) {
            cal.setTime(TimeUtils.getNow());
        } else {
            cal.setTime(date);
        }

        cal.add(2, months);
        return new Timestamp(cal.getTime().getTime());
    }

    public static void atualizarDataVigenciaContrato(BigDecimal numContrato, @Nullable Date dtVigencia) throws Exception {
        JapeSession.SessionHandle hnd = null;

        try {
            hnd = JapeSession.open();
            JapeWrapper conDAO = JapeFactory.dao("Contrato");
            JapeWrapper planoDAO = JapeFactory.dao("AD_CADPLAFUN");
            DynamicVO conVO = conDAO.findOne("NUMCONTRATO = ?", new Object[]{numContrato});
            DynamicVO planoVO = planoDAO.findOne("CODPLAN = ?", new Object[]{conVO.asBigDecimal("AD_CODPLAN")});
            Timestamp dtVigenciaFinal = addMonthstoDate((Timestamp)null, planoVO.asInt("VIGENCIA"));
            ((FluidUpdateVO)conDAO.prepareToUpdate(conVO).set("DTTERMINO", dtVigenciaFinal)).update();
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        } finally {
            JapeSession.close(hnd);
        }

    }

    public static String converterParaReal(BigDecimal real) throws Exception {
        String retorno = null;
        DecimalFormat dinheiro = new DecimalFormat("#,###,##0.00");
        if (real != null) {
            retorno = "R$ " + dinheiro.format(real);
        }

        return retorno;
    }

    public void escreveLog(String mensagem) {
        System.out.println(this.processName + " - " + mensagem);
    }
}
