package cn.gbtmedia.system.servcie;

import cn.gbtmedia.system.dto.QueryParam;
import cn.gbtmedia.system.entity.SysUser;
import cn.gbtmedia.system.repository.SystemUserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
@Service
public class SysUserService implements UserDetailsService {

    @Resource
    private SystemUserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    void init(){
        // 给个默认用户
        SysUser admin = userRepository.findByUsername("admin");
        if(admin == null){
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            userRepository.save(admin);
            log.info("init sysUser admin 123456");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = userRepository.findByUsername(username);
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public String getPassword() {
                return  sysUser.getPassword();
            }

            @Override
            public String getUsername() {
                return sysUser.getUsername();
            }
        };
    }

    public void updatePassword(SysUser params){
        SysUser sysUser = userRepository.findByUsername(params.getUsername());
        sysUser.setPassword(passwordEncoder.encode(params.getPassword()));
        userRepository.save(sysUser);
    }

    public Page<SysUser> page(QueryParam param) {
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize());
        return userRepository.findAll(page);
    }
}
