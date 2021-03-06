package com.kh.spring17.repository;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.kh.spring17.entity.CertDto;

 @Repository
 public class CertDaoImpl implements CertDao{

	@Autowired
	private SqlSession sqlSession;
	
	@Override
	public void insert(CertDto certDto) {
		sqlSession.insert("cert.insert2", certDto);
	}
	
	@Override
	public boolean check(CertDto certDto) {
		CertDto find=sqlSession.selectOne("cert.check",certDto);
		
		return find!=null;
	}
	
	@Override @Transactional
	public void deleteByEmail(String email) {
		sqlSession.delete("cert.deleteByEmail",email);
	}
	
	@Override
	public void clear() {
		sqlSession.delete("cert.deleteByTime");
	}
	
	
}