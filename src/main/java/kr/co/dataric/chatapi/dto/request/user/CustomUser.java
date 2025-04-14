package kr.co.dataric.chatapi.dto.request.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class CustomUser implements UserDetails {
	
	private final Long id;
	private final String userId;
	private final String password;
	private final boolean active;
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}
	
	@Override
	public String getUsername() {
		return userId;
	}
	
	@Override
	public boolean isAccountNonExpired() {
		return active;
	}
	
	@Override
	public boolean isAccountNonLocked() {
		return active;
	}
	
	@Override
	public boolean isCredentialsNonExpired() {
		return active;
	}
	
	@Override
	public boolean isEnabled() {
		return active;
	}
}
