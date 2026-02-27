package com.example.ecommerce.order.internal;

import com.example.ecommerce.order.flashsale.FlashSaleEvidenceService;
import org.springframework.web.bind.annotation.*;

/**
 * 內部證據輸出 API（不走 Gateway）
 *
 * 用途：
 * - 壓測後查詢 SUCCESS winners 與 FIFO 證據（enqueueSeq/successSeq）
 */
@RestController
@RequestMapping("/internal/flashsale")
public class FlashSaleInternalController {

    private final FlashSaleEvidenceService evidenceService;
    private final InternalApiAuth internalApiAuth;

    public FlashSaleInternalController(FlashSaleEvidenceService evidenceService, InternalApiAuth internalApiAuth) {
        this.evidenceService = evidenceService;
        this.internalApiAuth = internalApiAuth;
    }

    /**
     * winners evidence
     * - limit: 預設 50
     */
    @GetMapping("/{productId}/winners")
    public FlashSaleEvidenceService.WinnersResult winners(
            @PathVariable("productId") long productId,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "sinceSeconds", defaultValue = "0") long sinceSeconds
    ) {
        internalApiAuth.requireInternalToken();

        int resolvedLimit = Math.min(Math.max(limit, 1), 2000);
        return evidenceService.getWinners(productId, resolvedLimit, sinceSeconds);
    }
}