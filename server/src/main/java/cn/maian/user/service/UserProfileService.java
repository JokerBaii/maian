package cn.maian.user.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.dto.UserProfileResponse;
import cn.maian.user.dto.VerifyIdentityRequest;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserProfileService {

    public static final UUID CURRENT_USER_ID =
        UUID.fromString("30000000-0000-0000-0000-000000000001");

    private static final int[] ID_CARD_WEIGHTS = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
    private static final char[] ID_CARD_CHECK_CODES = { '1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2' };

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse current() {
        return UserProfileResponse.from(findCurrent());
    }

    @Transactional
    public UserProfileResponse verify(VerifyIdentityRequest request) {
        String idCard = request.idCard().toUpperCase(Locale.ROOT);
        if (!hasValidChecksum(idCard)) {
            throw new IllegalArgumentException("身份证号校验失败");
        }
        var profile = findCurrent();
        profile.verify(request.realName().trim(), maskIdCard(idCard));
        return UserProfileResponse.from(profile);
    }

    private boolean hasValidChecksum(String idCard) {
        int sum = 0;
        for (int index = 0; index < 17; index++) {
            sum += Character.digit(idCard.charAt(index), 10) * ID_CARD_WEIGHTS[index];
        }
        return ID_CARD_CHECK_CODES[sum % 11] == idCard.charAt(17);
    }

    private String maskIdCard(String idCard) {
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    private cn.maian.user.domain.UserProfile findCurrent() {
        return userProfileRepository.findById(CURRENT_USER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("用户资料不存在"));
    }
}
