package demo.bigwork.service.Impl;

import demo.bigwork.crypto.SearchTokenHasher;
import demo.bigwork.dao.UserSearchTokenDAO;
import demo.bigwork.model.enums.SearchableField;
import demo.bigwork.model.po.UserPO;
import demo.bigwork.model.po.UserSearchTokenPO;
import demo.bigwork.service.UserSearchIndexService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserSearchIndexServiceImpl implements UserSearchIndexService {

    private final UserSearchTokenDAO tokenDAO;
    private final SearchTokenHasher hasher;

    public UserSearchIndexServiceImpl(UserSearchTokenDAO tokenDAO, SearchTokenHasher hasher) {
        this.tokenDAO = tokenDAO;
        this.hasher = hasher;
    }

    @Override
    @Transactional
    public void reindex(UserPO user) {
        tokenDAO.deleteByUserId(user.getUserId());

        List<UserSearchTokenPO> tokens = new ArrayList<>();
        for (String hash : hasher.hashAll(user.getPhone())) {
            tokens.add(new UserSearchTokenPO(user.getUserId(), SearchableField.PHONE, hash));
        }
        for (String hash : hasher.hashAll(user.getDefaultAddress())) {
            tokens.add(new UserSearchTokenPO(user.getUserId(), SearchableField.ADDRESS, hash));
        }
        tokenDAO.saveAll(tokens);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findCandidateUserIds(String keyword) {
        Set<String> hashes = hasher.hashAll(keyword);
        if (hashes.isEmpty()) {
            return Set.of();
        }
        Set<Long> candidates = new HashSet<>();
        candidates.addAll(tokenDAO.findUserIdsByFieldAndTokenHashIn(SearchableField.PHONE, hashes));
        candidates.addAll(tokenDAO.findUserIdsByFieldAndTokenHashIn(SearchableField.ADDRESS, hashes));
        return candidates;
    }
}
