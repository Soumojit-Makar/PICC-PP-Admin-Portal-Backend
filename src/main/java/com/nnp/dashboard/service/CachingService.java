package com.nnp.dashboard.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nnp.dashboard.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.nnp.dashboard.vo.UserConfigVO;

import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around the Spring {@link CacheManager} used to:
 *   - evict user-access cache entries keyed by (env/fea/elem/detail + userId)
 *   - temporarily cache a newly registered account's password under a dedicated
 *     {@code acc-pwd-cache} region, so it can be consumed exactly once when the
 *     environment provisioning kicks off after payment is confirmed
 *     ( {@link #savePassword} / {@link #retrieveAndDeletePassword} ).
 */
@Service
@Slf4j
public class CachingService {

	@Autowired
	CacheManager cacheManager;

	private void evictSingleCacheValue(String cacheName, String cacheKey) {
        Cache cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.evict(cacheKey);
        } else {
            log.warn("Cache not found: {}", Utils.sanitizeForLog(cacheName));
        }
	}

	public void evictAllCacheValues(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }else {
            log.warn("Cache not found: {}", Utils.sanitizeForLog(cacheName));
        }
	}

	public void evictUserAccessCache(List<UserConfigVO> userConfVOs) {
//		log.info("in CachingService -> evictUserAccessCache() ");
		Set<String> cacheKeySet = new HashSet<String>();
    
		userConfVOs.forEach(userConfVO -> {
//			log.info("in CachingService -> evictUserAccessCache():: " +userConfVO);
			if(userConfVO.getEnvCode() != null && !cacheKeySet.contains(userConfVO.getEnvCode()+"-"+userConfVO.getUserId())) {
				cacheKeySet.add(userConfVO.getEnvCode()+"-"+userConfVO.getUserId());
			}

			if(userConfVO.getFeaId() != null && !cacheKeySet.contains(userConfVO.getFeaId()+"-"+userConfVO.getUserId())) {
				cacheKeySet.add(userConfVO.getFeaId()+"-"+userConfVO.getUserId());

			}
			if(userConfVO.getElemId() != null && !cacheKeySet.contains(userConfVO.getElemId()+"-"+userConfVO.getUserId())) {
				cacheKeySet.add(userConfVO.getElemId()+"-"+userConfVO.getUserId());

			}
			if(userConfVO.getElmDetailId() != null && !cacheKeySet.contains(userConfVO.getElmDetailId()+"-"+userConfVO.getUserId())) {
				cacheKeySet.add(userConfVO.getElmDetailId()+"-"+userConfVO.getUserId());

			}

		});

		/*
		 * clear the user access cache
		 */
		cacheKeySet.forEach(key->{
			evictSingleCacheValue("user-cache", key);
		});
		
        evictSingleCacheValue("user-cache", userConfVOs.get(0).getEnvCode());
	}
	/**
	 * Save admin password temporarily ( for env provisioning after payment ).
	 * User a dedicated "acc-pwd-cache" cache region - separate from user-cache.
	 */
	public void  savePassword(String accName, String password){
		Cache cache =cacheManager.getCache("acc-pwd-cache");
		if (cache !=null){
			cache.put(accName,password);
		}
//		log.info("Password cached for account : {}",accName);
	}
	/**
	 * Retrieve password and immediately evict - one-time use.
	 */
	public String retrieveAndDeletePassword(String accName){
		Cache cache = cacheManager.getCache("acc-pwd-cache");
		if (cache == null) return null;
		Cache.ValueWrapper wrapper = cache.get(accName);
		if (wrapper == null) {
//			log.warn("No cached password found for account: {}", accName);
			return null;
		}
		cache.evict(accName);
//		log.info("Password retrieved and evicted from cache for account: {} --- {}", accName,(String) wrapper.get());
		return (String) wrapper.get();
	}

}
