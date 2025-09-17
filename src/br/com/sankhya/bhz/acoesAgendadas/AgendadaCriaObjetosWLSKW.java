package br.com.sankhya.bhz.acoesAgendadas;

import br.com.sankhya.doit.entities.CadasdepEntity;
import br.com.sankhya.doit.entities.TaxasContratoDependente;
import br.com.sankhya.doit.utils.UtilitariosDOit;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;
import com.sankhya.util.JdbcUtils;
import com.sankhya.util.TimeUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

public class AgendadaCriaObjetosWLSKW implements ScheduledAction {
    UtilitariosDOit utilitariosDOit = new UtilitariosDOit();
    JapeWrapper parcDAO = JapeFactory.dao("Parceiro");
    JapeWrapper cttDAO = JapeFactory.dao("Contato");
    JapeWrapper conDAO = JapeFactory.dao("Contrato");
    JapeWrapper cidDAO = JapeFactory.dao("Cidade");
    JapeWrapper baiDAO = JapeFactory.dao("Bairro");
    JapeWrapper endDAO = JapeFactory.dao("Endereco");
    JapeWrapper parametroSistemaDAO = JapeFactory.dao("ParametroSistema");
    JapeWrapper ppgDAO = JapeFactory.dao("ParcelaPagamento");
    JapeWrapper depConDAO = JapeFactory.dao("AD_CADASDEP");
    JapeWrapper conWLDAO = JapeFactory.dao("AD_CONWEBLUTO");
    BigDecimal empresaPadrao = new BigDecimal(5);
    DynamicVO defaultSellerParamVO;
    BigDecimal defaultSellerId;
    BigDecimal idTipVendaAdesao;
    BigDecimal unidadeNegocio;
    boolean onlyAdhesion;
    DynamicVO partnerVO;
    DynamicVO contractVO;
    DynamicVO financeiroVO;
    DynamicVO sellerVO;
    DynamicVO cidVO;
    DynamicVO baiVO;
    DynamicVO endVO;
    BigDecimal idPartner;
    BigDecimal contractId;
    BigDecimal numContrato;
    BigDecimal idCentroResultado;
    JdbcWrapper jdbc;
    NativeSql sql;
    ResultSet rset;
    JapeSession.SessionHandle hnd;
    EntityFacade entity;

    public AgendadaCriaObjetosWLSKW() throws Exception {
        this.defaultSellerParamVO = this.parametroSistemaDAO.findOne("CHAVE = ?", new Object[]{"DFTSELLERID"});
        this.defaultSellerId = BigDecimal.ZERO;
        this.idTipVendaAdesao = new BigDecimal(813);
        this.unidadeNegocio = BigDecimal.ZERO;
        this.onlyAdhesion = false;
        this.partnerVO = null;
        this.contractVO = null;
        this.financeiroVO = null;
        this.sellerVO = null;
        this.cidVO = null;
        this.baiVO = null;
        this.endVO = null;
        this.idPartner = null;
        this.contractId = null;
        this.numContrato = null;
        this.idCentroResultado = null;
        this.jdbc = null;
        this.sql = null;
        this.rset = null;
        this.hnd = null;
        this.entity = EntityFacadeFactory.getDWFFacade();
    }

    private static Map<Integer, Integer> getPlanoDePara() {
        Map<Integer, Integer> dePara = new HashMap();
        dePara.put(3, 1);
        dePara.put(4, 2);
        dePara.put(5, 4);
        dePara.put(7, 3);
        dePara.put(12, 7);
        dePara.put(13, 13);
        dePara.put(14, 19);
        dePara.put(16, 6);
        dePara.put(17, 44);
        dePara.put(2, 14);
        dePara.put(6, 30);
        return dePara;
    }

    public static Map<Integer, Integer> getRelacionamentoDePara() {
        Map<Integer, Integer> dePara = new HashMap();
        dePara.put(1, 11);
        dePara.put(2, 12);
        dePara.put(3, 7);
        dePara.put(4, 21);
        dePara.put(5, 1);
        dePara.put(6, 2);
        dePara.put(7, 3);
        dePara.put(8, 6);
        dePara.put(9, 18);
        dePara.put(10, 20);
        dePara.put(11, 19);
        dePara.put(12, 4);
        dePara.put(13, 15);
        dePara.put(14, 16);
        dePara.put(15, 23);
        dePara.put(16, 27);
        dePara.put(17, 8);
        dePara.put(18, 5);
        dePara.put(19, 17);
        dePara.put(22, 18);
        dePara.put(23, 10);
        dePara.put(25, 24);
        return dePara;
    }

    public static Map<Integer, Integer> getTaxaDePara() {
        Map<Integer, Integer> dePara = new HashMap();
        dePara.put(1, 3);
        dePara.put(2, 4);
        dePara.put(4, 22);
        dePara.put(5, 45);
        dePara.put(6, 43);
        dePara.put(17, 2);
        dePara.put(18, 2);
        dePara.put(19, 6);
        dePara.put(20, 5);
        dePara.put(21, 37);
        dePara.put(22, 69);
        return dePara;
    }

    private static Map<String, Integer> getTipoVendaDePara() {
        Map<String, Integer> dePara = new HashMap();
        dePara.put("boleto", 125);
        dePara.put("cartao", 2);
        dePara.put("pix", 813);
        dePara.put("dinheiro", 125);
        return dePara;
    }

    private List<Map<String, Object>> makeGeneralSelect(StringBuilder select) throws Exception {
        List<Map<String, Object>> results = new ArrayList();
        this.jdbc = this.entity.getJdbcWrapper();
        this.jdbc.openSession();
        this.sql = new NativeSql(this.jdbc);
        this.sql.appendSql(select.toString());
        this.rset = this.sql.executeQuery();

        try {
            ResultSetMetaData metaData = this.rset.getMetaData();
            int columnCount = metaData.getColumnCount();

            while(this.rset.next()) {
                Map<String, Object> row = new HashMap();

                for(int i = 1; i <= columnCount; ++i) {
                    String columnName = metaData.getColumnName(i);
                    Object value = this.rset.getObject(i);
                    row.put(columnName, value);
                }

                results.add(row);
            }
        } finally {
            if (this.rset != null) {
                this.rset.close();
            }

        }

        return results;
    }

    private Collection<DynamicVO> getContractsToCreate() throws Exception {
        return this.conWLDAO.find("NUMCONTRATO IS NULL");
    }

    private void setContractAsProcessed(DynamicVO contractToIntegrate) throws Exception {
        ((FluidUpdateVO)((FluidUpdateVO)((FluidUpdateVO)this.conWLDAO.prepareToUpdate(contractToIntegrate).set("NUMCONTRATO", this.contractVO.asBigDecimal("NUMCONTRATO"))).set("DHCONTSKW", TimeUtils.getNow())).set("DTULTTENT", TimeUtils.getNow())).update();
    }

    private List<Map<String, Object>> getContractInfoWebLuto() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT * ");
        queryBuilder.append(" FROM OPENQUERY([MYSQL], 'SELECT * FROM metropax_banco.contracts WHERE ID = ").append(this.contractId).append("') ");
        return this.makeGeneralSelect(queryBuilder);
    }

    private List<Map<String, Object>> getPartnerInfoWebLuto() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT * ");
        queryBuilder.append(" FROM OPENQUERY([MYSQL], 'SELECT * FROM metropax_banco.z_integracao_loja_planos_partner WHERE ID = ").append(this.idPartner).append("') ");
        return this.makeGeneralSelect(queryBuilder);
    }

    private List<Map<String, Object>> getTaxasInfoWebLuto() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT * ");
        queryBuilder.append(" FROM OPENQUERY([MYSQL], 'SELECT F.*, BF.name as beneficiary_name FROM metropax_banco.z_integracao_loja_planos_contract_benefits F LEFT JOIN metropax_banco.z_integracao_loja_planos_beneficiarie BF on BF.ID = F.beneficiary_id WHERE F.contract_id = ").append(this.contractId).append("') ");
        return this.makeGeneralSelect(queryBuilder);
    }

    private List<Map<String, Object>> getFinancialInfoWebLuto() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT * ");
        queryBuilder.append(" FROM OPENQUERY([MYSQL], 'SELECT F.*, C.cupom_id FROM metropax_banco.z_integracao_loja_planos_contract_finance F INNER JOIN metropax_banco.contracts C ON C.ID = F.contract_id WHERE C.ID = ").append(this.contractId).append("') ");
        return this.makeGeneralSelect(queryBuilder);
    }

    private List<Map<String, Object>> getDependentsInfoWebLuto() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT * ");
        queryBuilder.append(" FROM OPENQUERY([MYSQL], 'SELECT * FROM metropax_banco.z_integracao_loja_planos_beneficiarie WHERE contract_id = ").append(this.contractId).append("') ");
        return this.makeGeneralSelect(queryBuilder);
    }

    public DynamicVO createPartnerOnSankhya(Map<String, Object> partnerData) throws Exception {
        JapeSession.SessionHandle hnd = null;

        try {
            hnd = JapeSession.open();
            this.partnerVO = this.parcDAO.findOne("CGC_CPF = ?", new Object[]{partnerData.get("CGC_CPF")});
            if (this.partnerVO == null) {
                FluidCreateVO parcNewVO = this.parcDAO.create();

                for(Map.Entry<String, Object> entry : partnerData.entrySet()) {
                    String key = (String)entry.getKey();
                    Object value = entry.getValue();
                    parcNewVO.set(key, value);
                }

                this.partnerVO = parcNewVO.save();
            }

            DynamicVO cttVO = null;
            if (cttVO == null) {
                String nomeContato = this.partnerVO.asString("NOMEPARC");
                nomeContato = nomeContato.substring(0, Math.min(nomeContato.length(), 40));
                cttVO = ((FluidCreateVO)((FluidCreateVO)this.cttDAO.create().set("CODPARC", this.partnerVO.asBigDecimal("CODPARC"))).set("NOMECONTATO", nomeContato)).save();
            }

            DynamicVO var17 = this.partnerVO;
            return var17;
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        } finally {
            JapeSession.close(hnd);
        }

        return this.partnerVO;
    }

    private Map<String, Object> preparePartnerData(List<Map<String, Object>> partnerInfo) throws Exception {
        Map<String, Object> partnerData = new HashMap();
        String codigoIBGE = ((Map)partnerInfo.get(0)).get("codigoIBGE").toString();
        System.out.println("codigoIBGE: " + codigoIBGE);
        DynamicVO cityVO = this.cidDAO.findOne("CODMUNFIS = ?", new Object[]{new BigDecimal(codigoIBGE)});
        String neighborhood = ((Map)partnerInfo.get(0)).get("neighborhood").toString();
        String street = ((Map)partnerInfo.get(0)).get("street").toString();
        Collection<DynamicVO> bairros = this.baiDAO.find("NOMEBAI = ?", new Object[]{neighborhood});
        if (!bairros.isEmpty()) {
            this.baiVO = (DynamicVO)bairros.iterator().next();
        } else {
            FluidCreateVO baiVO = this.baiDAO.create();
            baiVO.set("NOMEBAI", neighborhood);
            baiVO.save();
        }

        Collection<DynamicVO> enderecos = this.endDAO.find("NOMEEND = ?", new Object[]{street});
        if (!enderecos.isEmpty()) {
            this.endVO = (DynamicVO)enderecos.iterator().next();
        } else {
            FluidCreateVO endVO = this.endDAO.create();
            endVO.set("NOMEEND", street);
            endVO.save();
        }

        partnerData.put("NOMEPARC", ((Map)partnerInfo.get(0)).get("name"));
        partnerData.put("CGC_CPF", ((Map)partnerInfo.get(0)).get("cgc_cpf"));
        partnerData.put("IDENTINSCESTAD", ((Map)partnerInfo.get(0)).get("rg"));
        partnerData.put("TIPPESSOA", "F");
        partnerData.put("CLIENTE", "S");
        partnerData.put("CEP", ((Map)partnerInfo.get(0)).get("cep"));
        partnerData.put("NUMEND", ((Map)partnerInfo.get(0)).get("numero"));
        partnerData.put("COMPLEMENTO", ((Map)partnerInfo.get(0)).get("complement"));
        partnerData.put("DTNASC", ((Map)partnerInfo.get(0)).get("birth_date"));
        partnerData.put("EMAIL", ((Map)partnerInfo.get(0)).get("email"));
        partnerData.put("FAX", ((Map)partnerInfo.get(0)).get("cell_phone"));
        partnerData.put("AD_NOMESOCIAL", ((Map)partnerInfo.get(0)).get("social_name"));
        partnerData.put("CODCID", cityVO.asBigDecimal("CODCID"));
        partnerData.put("CODBAI", this.baiVO.asBigDecimal("CODBAI"));
        partnerData.put("CODEND", this.endVO.asBigDecimal("CODEND"));
        return partnerData;
    }

    private List<Map<String, Object>> prepareFinanceData(List<Map<String, Object>> financeInfoList) throws Exception {
        List<Map<String, Object>> financeDataList = new ArrayList();

        for(Map<String, Object> financeInfoMap : financeInfoList) {
            Map<String, Object> financeData = new HashMap();
            Date dueDate = (Date)financeInfoMap.get("due_date");
            Timestamp dtVenc = new Timestamp(dueDate.getTime());
            BigDecimal conta = null;
            BigDecimal banco = null;
            BigDecimal codNat = BigDecimal.valueOf(1010100L);
            BigDecimal codTipTit = new BigDecimal(50);
            BigDecimal vlrDesdob = new BigDecimal(financeInfoMap.get("amount").toString());
            String cupomId = null;
            if (financeInfoMap.get("cupom_id") != null) {
                cupomId = financeInfoMap.get("cupom_id").toString();
            }

            this.utilitariosDOit.escreveLog("Cupom ID: " + cupomId);
            if (cupomId != null && cupomId.equals("8")) {
                vlrDesdob = new BigDecimal(79.9);
                this.onlyAdhesion = true;
                this.utilitariosDOit.escreveLog("Cupom ID: " + cupomId + " é 8, então onlyAdhesion é true");
            } else {
                this.onlyAdhesion = false;
                this.utilitariosDOit.escreveLog("Cupom ID: " + cupomId + " não é 8, então onlyAdhesion é false");
            }

            DynamicVO ppgVO = this.ppgDAO.findOne("CODEMP = ? AND CODTIPVENDA = ?", new Object[]{this.empresaPadrao, this.idTipVendaAdesao});
            if (ppgVO != null) {
                conta = ppgVO.asBigDecimal("CODCTABCOINT");
                banco = ppgVO.asBigDecimal("CODBCOPAD");
                codNat = ppgVO.asBigDecimalOrZero("CODNATPAD");
                codTipTit = ppgVO.asBigDecimalOrZero("CODTIPTITPAD");
            }

            financeData.put("CODPARC", this.partnerVO.asBigDecimal("CODPARC"));
            financeData.put("NUMCONTRATO", this.numContrato);
            financeData.put("CODEMP", this.empresaPadrao);
            financeData.put("DTVENC", dtVenc);
            financeData.put("DTVENCINIC", dtVenc);
            financeData.put("CODCENCUS", this.idCentroResultado);
            financeData.put("VLRDESDOB", vlrDesdob);
            financeData.put("NUMNOTA", new BigDecimal(financeInfoMap.get("contract_id").toString()));
            financeData.put("DTNEG", TimeUtils.getNow());
            financeData.put("DHMOV", TimeUtils.getNow());
            financeData.put("CODNAT", codNat);
            financeData.put("CODCTABCOINT", conta);
            financeData.put("CODBCO", banco);
            financeData.put("ORIGEM", "F");
            financeData.put("PROVISAO", "N");
            financeData.put("AD_ADESAO", "S");
            financeData.put("RECDESP", BigDecimal.ONE);
            financeData.put("CODTIPTIT", codTipTit);
            financeDataList.add(financeData);
            if (this.onlyAdhesion) {
                break;
            }
        }

        return financeDataList;
    }

    private List<TaxasContratoDependente> prepareTaxaData() throws Exception {
        List<Map<String, Object>> taxasContratoInfo = this.getTaxasInfoWebLuto();
        List<TaxasContratoDependente> taxas = new ArrayList();

        for(Map<String, Object> mapObj : taxasContratoInfo) {
            Integer service = (new BigDecimal(mapObj.get("service_id").toString())).intValue();
            this.utilitariosDOit.escreveLog("Service here: " + service.toString());
            Integer codTaxaSankhya = (Integer)getTaxaDePara().get(service);
            this.utilitariosDOit.escreveLog("Cod Taxa Sankhya.: " + codTaxaSankhya.toString());
            BigDecimal coditoTaxaSankhya = (BigDecimal)Optional.ofNullable(codTaxaSankhya).map((c) -> new BigDecimal(c.toString())).orElse(BigDecimal.ONE);
            TaxasContratoDependente taxa = new TaxasContratoDependente();
            taxa.valorTaxa = new BigDecimal(mapObj.get("benefit_amount").toString());
            taxa.origemSimulacao = "S";
            taxa.codigoTaxa = coditoTaxaSankhya;
            if (mapObj.get("beneficiary_name") != null) {
                taxa.nomeDependente = mapObj.get("beneficiary_name").toString();
            } else {
                taxa.nomeDependente = "";
            }

            taxas.add(taxa);
        }

        return taxas;
    }

    private Map<String, Object> prepareContractData(List<Map<String, Object>> contractInfo) throws Exception {
        Map<String, Object> contractData = new HashMap();
        BigDecimal idPlanoWebLuto = new BigDecimal(((Map)contractInfo.get(0)).get("plan_id").toString());
        Object paymentValue = ((Map)contractInfo.get(0)).get("payment");
        String tipoPaymentWebLuto = paymentValue != null ? paymentValue.toString() : null;
        String normalizedTipoPayment = tipoPaymentWebLuto != null ? tipoPaymentWebLuto.trim().toLowerCase(Locale.ROOT) : null;
        Map<Integer, Integer> planoDePara = getPlanoDePara();
        Map<String, Integer> tipoVendaDePara = getTipoVendaDePara();
        Integer codPlanSankhya = (Integer)planoDePara.get(idPlanoWebLuto != null ? idPlanoWebLuto.intValue() : null);
        Integer codTipoVenda = normalizedTipoPayment != null ? (Integer)tipoVendaDePara.get(normalizedTipoPayment) : null;
        if (codTipoVenda == null) {
            this.utilitariosDOit.escreveLog("Tipo de pagamento recebido sem correspondência: " + tipoPaymentWebLuto);
            throw new RuntimeException("Tipo de pagamento WebLuto [" + tipoPaymentWebLuto + "] sem correspondência no mapa de/para.");
        }
        System.out.println("Cod Tipo venda: " + codTipoVenda);
        if (codPlanSankhya == null) {
            throw new RuntimeException("Plano WebLuto ID [" + idPlanoWebLuto + "] sem correspondência no mapa de/para.");
        } else {
            if (this.defaultSellerParamVO != null) {
                this.defaultSellerId = new BigDecimal(this.defaultSellerParamVO.asString("TEXTO"));
            }

            BigDecimal idVendedor = BigDecimal.ZERO;
            if (((Map)contractInfo.get(0)).get("seller_id") == null) {
                idVendedor = this.defaultSellerId;
            } else {
                idVendedor = new BigDecimal(((Map)contractInfo.get(0)).get("seller_id").toString());
            }

            JapeWrapper vendDAO = JapeFactory.dao("Vendedor");
            DynamicVO vendedorVO = vendDAO.findOne("CODVEND = ?", new Object[]{idVendedor});
            if (vendedorVO != null) {
                idVendedor = vendedorVO.asBigDecimal("CODVEND");
            }

            this.unidadeNegocio = UtilitariosDOit.getUnidadeNegocio(this.partnerVO.asBigDecimalOrZero("CODCID"), idVendedor);
            BigDecimal priceLiquid = (BigDecimal)Optional.ofNullable(((Map)contractInfo.get(0)).get("price_liquid")).map((v) -> new BigDecimal(v.toString())).orElse(BigDecimal.ZERO);
            contractData.put("CODPARC", this.partnerVO.asBigDecimal("CODPARC"));
            contractData.put("CODEMP", this.empresaPadrao);
            contractData.put("AD_CODUNN", this.unidadeNegocio);
            contractData.put("AD_CODPLAN", new BigDecimal(codPlanSankhya));
            contractData.put("DTCONTRATO", TimeUtils.getNow());
            contractData.put("RECDESP", BigDecimal.ONE);
            contractData.put("DIAPAG", new BigDecimal(((Map)contractInfo.get(0)).get("payment_day").toString()));
            contractData.put("ATIVO", "S");
            contractData.put("AD_CODVEND", idVendedor);
            contractData.put("CODTIPVENDA", new BigDecimal(codTipoVenda));
            contractData.put("AD_PRECONTRATO", new BigDecimal(((Map)contractInfo.get(0)).get("id").toString()));
            contractData.put("AD_NUECOMMERCE", new BigDecimal(((Map)contractInfo.get(0)).get("id").toString()));
            contractData.put("PRECO_CONTRATO", priceLiquid);
            contractData.put("AD_CODFONTE", new BigDecimal(35));
            return contractData;
        }
    }

    private List<CadasdepEntity> prepareDependentsData() throws Exception {
        this.utilitariosDOit.escreveLog("Iniciando processo de preparação dos dados dos dependentes...");
        List<Map<String, Object>> dependentsInfo = this.getDependentsInfoWebLuto();
        List<CadasdepEntity> dependentes = new ArrayList();

        for(Map<String, Object> map : dependentsInfo) {
            for(Map.Entry<String, Object> entry : map.entrySet()) {
                this.utilitariosDOit.escreveLog("Chave: " + (String)entry.getKey() + " - Valor: " + entry.getValue());
            }

            CadasdepEntity dep = new CadasdepEntity();
            Date dataInclusao = (Date)map.get("included_date");
            Date dependenteDataNascimento = (Date)map.get("birth_date");
            Object kinshipObj = map.get("kinship");
            this.utilitariosDOit.escreveLog("Kinship here: " + kinshipObj.toString());
            Integer kinshipInt = (new BigDecimal(kinshipObj.toString())).intValue();
            if (kinshipInt == 0) {
                this.utilitariosDOit.escreveLog("Kinship is 0, skipping...");
            } else {
                Integer codRelac = (Integer)getRelacionamentoDePara().get(kinshipInt);
                this.utilitariosDOit.escreveLog("Cod Relac.: " + codRelac.toString());
                BigDecimal codigoParentesco = BigDecimal.ONE;
                if (codRelac != null) {
                    codigoParentesco = new BigDecimal(codRelac.toString());
                }

                dep.numeroContrato = this.numContrato;
                dep.nomeDependente = map.get("name").toString();
                dep.tipoDocumento = "CPF";
                dep.ativo = "S";
                dep.numeroDocumento = map.get("doc_number").toString();
                dep.dataInclusao = new Timestamp(dataInclusao.getTime());
                dep.codigoParentesco = codigoParentesco;
                dep.dataNascimento = new Timestamp(dependenteDataNascimento.getTime());
                this.utilitariosDOit.escreveLog("Dependente adicionado à lista: " + dep.nomeDependente);
                dependentes.add(dep);
            }
        }

        return dependentes;
    }

    public void onTime(ScheduledActionContext arg0) {
        ServiceContext sctx = new ServiceContext((HttpServletRequest)null);
        sctx.setAutentication(AuthenticationInfo.getCurrent());
        sctx.makeCurrent();

        try {
            SPBeanUtils.setupContext(sctx);
        } catch (Exception e) {
            e.printStackTrace();
            arg0.info("Error: Não foi Possivel Executar a Chamada SPBeanUtils.setupContext \n" + e.getMessage());
        }

        try {
            this.hnd = JapeSession.open();
            this.utilitariosDOit.processName = "AgendadaCriaObjetosWLSKW";
            this.utilitariosDOit.processId = UUID.randomUUID().toString();
            this.utilitariosDOit.escreveLog("Iniciando processo de criação de objetos...");

            for(DynamicVO contractToIntegrate : this.getContractsToCreate()) {
                try {
                    this.partnerVO = null;
                    this.idPartner = contractToIntegrate.asBigDecimal("PARTNERID");
                    this.contractId = contractToIntegrate.asBigDecimal("CODCONTRATO");
                    this.utilitariosDOit.processId = UUID.randomUUID().toString();
                    this.utilitariosDOit.escreveLog("Iniciando processo de criação de objetos para o contrato: " + this.contractId.toString());
                    List<Map<String, Object>> contractInfo = this.getContractInfoWebLuto();
                    List<Map<String, Object>> partnerInfo = this.getPartnerInfoWebLuto();
                    List<Map<String, Object>> financialInfo = this.getFinancialInfoWebLuto();
                    this.sellerVO = this.utilitariosDOit.retornaVendedor(this.defaultSellerId);
                    this.idCentroResultado = this.sellerVO.asBigDecimal("CODCENCUSPAD");
                    this.partnerVO = this.createPartnerOnSankhya(this.preparePartnerData(partnerInfo));
                    Map<String, Object> contractData = this.prepareContractData(contractInfo);
                    List<CadasdepEntity> dependentes = this.prepareDependentsData();
                    this.contractVO = this.utilitariosDOit.insereContrato((BigDecimal)null, (BigDecimal)contractData.get("AD_CODPLAN"), (BigDecimal)contractData.get("CODPARC"), (BigDecimal)contractData.get("AD_CODVEND"), (BigDecimal)contractData.get("DIAPAG"), (BigDecimal)contractData.get("AD_CODUNN"), this.empresaPadrao, (BigDecimal)contractData.get("CODTIPVENDA"), (Boolean)null, (BigDecimal)null, (BigDecimal)contractData.get("AD_PRECONTRATO"), dependentes, (BigDecimal)contractData.get("PRECO_CONTRATO"), (BigDecimal)contractData.get("AD_PRECONTRATO"));
                    this.numContrato = this.contractVO.asBigDecimal("NUMCONTRATO");
                    this.utilitariosDOit.numContrato = this.numContrato;
                    List<Map<String, Object>> financeDataList = this.prepareFinanceData(financialInfo);
                    List<DynamicVO> financeListToMarkAsPaid = new ArrayList();

                    for(Map<String, Object> financeDataMap : financeDataList) {
                        this.financeiroVO = this.utilitariosDOit.insereFinanceiroContrato(financeDataMap);
                        financeListToMarkAsPaid.add(this.financeiroVO);
                        this.utilitariosDOit.escreveLog("Financeiro de adesão inserido na lista para baixar com sucesso: " + this.financeiroVO.asBigDecimal("NUFIN").toString());
                        this.utilitariosDOit.escreveLog("Financeiro de adesão inserido com sucesso.");
                    }

                    Map<String, Object> mensalidadeFinanceDataMap = (Map)financeDataList.get(0);
                    BigDecimal contractPriceLiquid = (BigDecimal)contractData.get("PRECO_CONTRATO");
                    mensalidadeFinanceDataMap.put("VLRDESDOB", contractPriceLiquid);
                    mensalidadeFinanceDataMap.put("AD_ADESAO", "N");
                    DynamicVO mensalidadeFinanceiroVO = this.utilitariosDOit.insereFinanceiroContrato(mensalidadeFinanceDataMap);
                    this.utilitariosDOit.escreveLog("Only Adhesion here: " + this.onlyAdhesion);
                    if (!this.onlyAdhesion) {
                        financeListToMarkAsPaid.add(mensalidadeFinanceiroVO);
                        this.utilitariosDOit.escreveLog("Financeiro de primeira mensalidade inserido na lista para baixar com sucesso: " + mensalidadeFinanceiroVO.asBigDecimal("NUFIN").toString());
                        this.utilitariosDOit.escreveLog("Financeiro de primeira mensalidade inserido com sucesso.");
                    } else {
                        this.utilitariosDOit.escreveLog("Only Adhesion is true, skipping...");
                    }

                    this.utilitariosDOit.escreveLog("Iniciando processo de baixa dos financeiros...");
                    this.utilitariosDOit.escreveLog("Quantidade de financeiros para baixar: " + financeListToMarkAsPaid.size());
                    this.utilitariosDOit.escreveLog("Lista de financeiros para baixar: " + financeListToMarkAsPaid.toString());

                    for(DynamicVO financeVO : financeListToMarkAsPaid) {
                        this.utilitariosDOit.escreveLog("Iniciando baixa de financeiro: " + financeVO.asBigDecimal("NUFIN").toString());

                        try {
                            this.utilitariosDOit.baixaFinanceiro(financeVO);
                            this.utilitariosDOit.escreveLog("Financeiro baixado com sucesso.");
                        } catch (Exception e) {
                            this.utilitariosDOit.escreveLog("Erro ao baixar financeiro: " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }

                    this.utilitariosDOit.escreveLog("Processo de baixa dos financeiros concluído.");
                    List<TaxasContratoDependente> taxaContratoDependenteData = this.prepareTaxaData();
                    this.utilitariosDOit.escreveLog("Iniciando processo de inserção das taxas contrato dependente...");

                    for(TaxasContratoDependente taxaContratoDependente : taxaContratoDependenteData) {
                        taxaContratoDependente.numeroContrato = this.numContrato;
                        if (taxaContratoDependente.nomeDependente != null && !taxaContratoDependente.nomeDependente.isEmpty()) {
                            DynamicVO dependenteContrato = this.depConDAO.findOne("NUMCONTRATO = ? AND NOMEDEP = ?", new Object[]{this.numContrato, taxaContratoDependente.nomeDependente});
                            this.utilitariosDOit.escreveLog("Dependente para o contrato: " + this.numContrato + " - " + taxaContratoDependente.nomeDependente);
                            if (dependenteContrato != null) {
                                this.utilitariosDOit.escreveLog("Dependente encontrado: " + dependenteContrato.asString("NOMEDEP"));
                                taxaContratoDependente.codigoDependente = dependenteContrato.asBigDecimal("CODDEP");
                                this.utilitariosDOit.escreveLog("Taxa associada ao dependente: " + taxaContratoDependente.nomeDependente);
                            }
                        } else {
                            this.utilitariosDOit.escreveLog("Dependente não encontrado, associando taxa diretamente ao contrato.");
                        }

                        taxaContratoDependente.create();
                        this.utilitariosDOit.escreveLog("Taxa contrato dependente inserida com sucesso.");
                    }

                    this.setContractAsProcessed(contractToIntegrate);
                    this.utilitariosDOit.escreveLog("Contrato processado com sucesso.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtils.closeResultSet(this.rset);
            NativeSql.releaseResources(this.sql);
            JdbcWrapper.closeSession(this.jdbc);
            JapeSession.close(this.hnd);
            this.utilitariosDOit.escreveLog("Sessão fechada com sucesso.");
        }

    }
}

