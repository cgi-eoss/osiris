package com.cgi.eoss.osiris.costing;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.expression.ExpressionParser;
import org.springframework.transaction.annotation.Transactional;
import com.cgi.eoss.osiris.model.CostingExpression;
import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.Wallet;
import com.cgi.eoss.osiris.model.WalletTransaction;
import com.cgi.eoss.osiris.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.osiris.persistence.service.DatabasketDataService;
import com.cgi.eoss.osiris.persistence.service.WalletDataService;
import com.google.common.base.Strings;

/**
 * <p>Default implementation of {@link CostingService}.</p>
 */
public class CostingServiceImpl implements CostingService {

    private final ExpressionParser expressionParser;
    private final CostingExpressionDataService costingDataService;
    private final WalletDataService walletDataService;
    private final CostingExpression defaultJobCostingExpression;
    private final CostingExpression defaultDownloadCostingExpression;
	private DatabasketDataService databasketDataService;

    public CostingServiceImpl(ExpressionParser costingExpressionParser, CostingExpressionDataService costingDataService,
                              WalletDataService walletDataService, DatabasketDataService databasketDataService, String defaultJobCostingExpression, String defaultDownloadCostingExpression) {
        this.expressionParser = costingExpressionParser;
        this.costingDataService = costingDataService;
        this.walletDataService = walletDataService;
        this.databasketDataService = databasketDataService;
        this.defaultJobCostingExpression = CostingExpression.builder().costExpression(defaultJobCostingExpression).build();
        this.defaultDownloadCostingExpression = CostingExpression.builder().costExpression(defaultDownloadCostingExpression).build();
    }

    @Override
    public Integer estimateJobCost(JobConfig jobConfig) {
        int singleJobCost =  estimateSingleRunJobCost(jobConfig);
        if (jobConfig.getService().getType() == OsirisService.Type.PARALLEL_PROCESSOR) {
        		return calculateNumberOfInputs(jobConfig.getInputs().get("parallelInputs")) * singleJobCost;
        }
        else {
        		return singleJobCost;
        }
    }
    
    @Override
    public Integer estimateSingleRunJobCost(JobConfig jobConfig) {
        CostingExpression costingExpression = getCostingExpression(jobConfig.getService());

        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();
        return ((Number) expressionParser.parseExpression(expression).getValue(jobConfig)).intValue();
    }
    
    

	private int calculateNumberOfInputs(Collection<String> inputUris){
		int inputCount = 1;
		for (String inputUri: inputUris) {
			if (inputUri.startsWith("osiris://databasket")) {
				Databasket dataBasket = getDatabasketFromUri(inputUri);
				inputCount = dataBasket.getFiles().size();
			}
			else {
				if(inputUri.contains((","))) {
					inputCount = inputUri.split(",").length;
				}
				else {
					inputCount = 1;
				}
			}
		}
		return inputCount;
		
	}
	
	private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
        		throw new RuntimeException("Failed to load databasket for URI: " + uri);
        }
        	Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        	Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new RuntimeException("Failed to load databasket for ID " + databasketId));
        	return databasket;
    }
    
    @Override
    public Integer estimateDownloadCost(OsirisFile osirisFile) {
        CostingExpression costingExpression = getCostingExpression(osirisFile);

        String expression = Strings.isNullOrEmpty(costingExpression.getEstimatedCostExpression())
                ? costingExpression.getCostExpression()
                : costingExpression.getEstimatedCostExpression();

        return ((Number) expressionParser.parseExpression(expression).getValue(osirisFile)).intValue();
    }

    @Override
    @Transactional
    public void chargeForJob(Wallet wallet, Job job) {
        CostingExpression costingExpression = getCostingExpression(job.getConfig().getService());

        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(job)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.JOB)
                .associatedId(job.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }

    @Override
    @Transactional
    public void refundUser(Wallet wallet, Job job) {
        int cost = estimateJobCost(job.getConfig());
        WalletTransaction walletTransaction =
                WalletTransaction.builder().wallet(walletDataService.refreshFull(wallet)).balanceChange(cost)
                        .type(WalletTransaction.Type.JOB).associatedId(job.getId()).transactionTime(LocalDateTime.now()).build();
        walletDataService.transact(walletTransaction);
    }

    @Override
    @Transactional
    public void chargeForDownload(Wallet wallet, OsirisFile osirisFile) {
        CostingExpression costingExpression = getCostingExpression(osirisFile);

        String expression = costingExpression.getCostExpression();

        int cost = ((Number) expressionParser.parseExpression(expression).getValue(osirisFile)).intValue();
        WalletTransaction walletTransaction = WalletTransaction.builder()
                .wallet(walletDataService.refreshFull(wallet))
                .balanceChange(-cost)
                .type(WalletTransaction.Type.DOWNLOAD)
                .associatedId(osirisFile.getId())
                .transactionTime(LocalDateTime.now())
                .build();
        walletDataService.transact(walletTransaction);
    }

    private CostingExpression getCostingExpression(OsirisService osirisService) {
        return Optional.ofNullable(costingDataService.getServiceCostingExpression(osirisService)).orElse(defaultJobCostingExpression);
    }

    private CostingExpression getCostingExpression(OsirisFile osirisFile) {
        return Optional.ofNullable(costingDataService.getDownloadCostingExpression(osirisFile)).orElse(defaultDownloadCostingExpression);
    }

}
