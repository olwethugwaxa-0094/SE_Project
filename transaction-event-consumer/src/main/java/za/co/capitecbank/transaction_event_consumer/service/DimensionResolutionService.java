package za.co.capitecbank.transaction_event_consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import za.co.capitecbank.transaction_event_consumer.domain.Authentication;
import za.co.capitecbank.transaction_event_consumer.domain.MerchantData;
import za.co.capitecbank.transaction_event_consumer.entity.*;
import za.co.capitecbank.transaction_event_consumer.repository.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DimensionResolutionService {

    private final DimClientRepository clientRepository;
    private final DimAccountRepository accountRepository;
    private final DimMerchantRepository merchantRepository;
    private final DimPaymentChannelRepository channelRepository;
    private final DimCardAuthStatusRepository cardAuthStatusRepository;
    private final DimTransactionAuthRepository transactionAuthRepository;

    // No @Transactional here — Spring Data JPA's saveAndFlush() provides its own transaction.
    // With no outer transaction, DataIntegrityViolationException is isolated to that mini-tx
    // and does NOT poison any caller transaction.

    public DimClientEntity resolveClient(Long cifNr) {
        return clientRepository.findByCifNrAndCurrentTrue(cifNr).orElseGet(() -> {
            try {
                log.info("Creating new dim_client for cifNr={}", cifNr);
                DimClientEntity entity = new DimClientEntity();
                entity.setCifNr(cifNr);
                entity.setEffectiveFrom(LocalDateTime.now());
                entity.setCurrent(true);
                entity.setHouseKeeping(buildHouseKeeping("dim_client:" + cifNr));
                return clientRepository.saveAndFlush(entity);
            } catch (DataIntegrityViolationException e) {
                log.debug("dim_client concurrent insert cifNr={}, reading winner row", cifNr);
                return clientRepository.findByCifNrAndCurrentTrue(cifNr)
                        .orElseThrow(() -> new IllegalStateException("dim_client missing after conflict cifNr=" + cifNr));
            }
        });
    }

    public DimAccountEntity resolveAccount(Long accountNr, Long cifNr, Integer branch) {
        return accountRepository.findByAccountNrAndCurrentTrue(accountNr).orElseGet(() -> {
            try {
                log.info("Creating new dim_account for accountNr={}", accountNr);
                DimAccountEntity entity = new DimAccountEntity();
                entity.setAccountNr(accountNr);
                entity.setCifNr(cifNr);
                entity.setBranch(branch);
                entity.setEffectiveFrom(LocalDateTime.now());
                entity.setCurrent(true);
                entity.setHouseKeeping(buildHouseKeeping("dim_account:" + accountNr));
                return accountRepository.saveAndFlush(entity);
            } catch (DataIntegrityViolationException e) {
                log.debug("dim_account concurrent insert accountNr={}, reading winner row", accountNr);
                return accountRepository.findByAccountNrAndCurrentTrue(accountNr)
                        .orElseThrow(() -> new IllegalStateException("dim_account missing after conflict accountNr=" + accountNr));
            }
        });
    }

    public DimMerchantEntity resolveMerchant(MerchantData merchantData) {
        if (merchantData == null || merchantData.merchantName() == null) {
            return merchantRepository.findByMerchantNameAndCurrentTrue("UNKNOWN")
                    .orElseThrow(() -> new IllegalStateException(
                            "UNKNOWN merchant sentinel not found — check V2 migration"));
        }

        String name = merchantData.merchantName();
        return merchantRepository.findByMerchantNameAndCurrentTrue(name).orElseGet(() -> {
            try {
                log.info("Creating new dim_merchant for merchantName={}", name);
                DimMerchantEntity entity = new DimMerchantEntity();
                entity.setMerchantName(name);
                entity.setMerchantDesc(merchantData.merchantDesc());
                entity.setMerchantCategoryCode(merchantData.merchantCategoryCode());
                entity.setCity(merchantData.city());
                entity.setProvince(merchantData.province());
                entity.setEffectiveFrom(LocalDateTime.now());
                entity.setCurrent(true);
                entity.setHouseKeeping(buildHouseKeeping("dim_merchant:" + name));
                return merchantRepository.saveAndFlush(entity);
            } catch (DataIntegrityViolationException e) {
                log.debug("dim_merchant concurrent insert merchantName={}, reading winner row", name);
                return merchantRepository.findByMerchantNameAndCurrentTrue(name)
                        .orElseThrow(() -> new IllegalStateException("dim_merchant missing after conflict merchantName=" + name));
            }
        });
    }

    public DimPaymentChannelEntity resolveChannel(String channelCode) {
        return channelRepository.findByChannelCode(channelCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown payment channel: %s — check dim_payment_channel seed data".formatted(channelCode)));
    }

    public DimTransactionAuthEntity resolveAuth(Authentication authentication) {
        if (authentication == null) return null;
        return transactionAuthRepository.findByAuthTraceIdAndCurrentTrue(authentication.authTraceId())
                .orElseGet(() -> {
                    if (authentication.cardAuthStatus() == null) return null;
                    DimCardAuthStatusEntity authStatus = cardAuthStatusRepository
                            .findByStatusCode(authentication.cardAuthStatus())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Unknown card auth status: %s — check dim_card_auth_status seed data"
                                            .formatted(authentication.cardAuthStatus())));
                    try {
                        log.info("Creating new dim_transaction_auth for authTraceId={}", authentication.authTraceId());
                        DimTransactionAuthEntity entity = new DimTransactionAuthEntity();
                        entity.setAuthTraceId(authentication.authTraceId());
                        entity.setCardAuthStatus(authStatus);
                        entity.setEffectiveFrom(LocalDateTime.now());
                        entity.setCurrent(true);
                        entity.setHouseKeeping(buildHouseKeeping("dim_auth:" + authentication.authTraceId()));
                        return transactionAuthRepository.saveAndFlush(entity);
                    } catch (DataIntegrityViolationException e) {
                        log.debug("dim_transaction_auth concurrent insert authTraceId={}, reading winner row", authentication.authTraceId());
                        return transactionAuthRepository.findByAuthTraceIdAndCurrentTrue(authentication.authTraceId())
                                .orElseThrow(() -> new IllegalStateException("dim_transaction_auth missing after conflict authTraceId=" + authentication.authTraceId()));
                    }
                });
    }

    private HouseKeeping buildHouseKeeping(String naturalKey) {
        return HouseKeeping.builder()
                .eBatchId(naturalKey)
                .eIngestId(UUID.randomUUID())
                .eOperation("INSERT")
                .eSourceSystem("transaction-event-consumer")
                .eRowHash(Integer.toHexString(naturalKey.hashCode()))
                .eLoadedAt(OffsetDateTime.now())
                .build();
    }
}
