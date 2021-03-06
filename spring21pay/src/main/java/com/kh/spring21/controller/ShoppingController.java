package com.kh.spring21.controller;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kh.spring21.entity.PaymentDto;
import com.kh.spring21.entity.ProductDto;
import com.kh.spring21.repository.PaymentDao;
import com.kh.spring21.repository.ProductDao;
import com.kh.spring21.service.PayService;
import com.kh.spring21.vo.ChunkVO;
import com.kh.spring21.vo.KakaoPayApprovePrepareVO;
import com.kh.spring21.vo.KakaoPayApproveVO;
import com.kh.spring21.vo.KakaoPayCancelPrepareVO;
import com.kh.spring21.vo.KakaoPayReadyPrepareVO;
import com.kh.spring21.vo.KakaoPayReadyVO;
import com.kh.spring21.vo.KakaoPaySearchVO;
import com.kh.spring21.vo.TestVO;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Controller
@RequestMapping("/shop")
public class ShoppingController {
	
	@Autowired
	private ProductDao productDao;
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("list", productDao.list());
		return "shop/home";
	}
	
	@GetMapping("/detail/{no}")
	public String detail(@PathVariable int no, Model model) {
		model.addAttribute("productDto", productDao.get(no));
		return "shop/detail";
	}
	
	@PostMapping("/buy")
	public String buy(
			@RequestParam int no, @RequestParam int quantity,
			Model model) {
		ProductDto productDto = productDao.get(no);
		model.addAttribute("productDto", productDto);
		model.addAttribute("quantity", quantity);		
		int total_amount = productDto.getPrice() * quantity;
		model.addAttribute("total_amount", total_amount);
		return "shop/buy";
	}
	
	@Autowired
	@Qualifier("kakaoPayService2")
	private PayService payService;
	
	@PostMapping("/confirm")
	public String confirm(
			HttpSession session, 
			@ModelAttribute KakaoPayReadyPrepareVO prepareVO
			) throws URISyntaxException {
		int memberNo = (int)session.getAttribute("memberNo");//???????????? ??????
		prepareVO.setPartner_user_id(String.valueOf(memberNo));
		
		KakaoPayReadyVO readyVO = payService.ready(prepareVO);
		
		session.setAttribute("partner_order_id", readyVO.getPartner_order_id());
		session.setAttribute("partner_user_id", readyVO.getPartner_user_id());
		session.setAttribute("tid", readyVO.getTid());
		
		return "redirect:"+readyVO.getNext_redirect_pc_url();
	}
	@GetMapping("/success")
	public String success(
			HttpSession session,
			@ModelAttribute KakaoPayApprovePrepareVO prepareVO) throws URISyntaxException {
		//???????????? ???????????? ?????? ??? ??????
		prepareVO.setPartner_order_id((String)session.getAttribute("partner_order_id"));
		prepareVO.setPartner_user_id((String)session.getAttribute("partner_user_id"));
		prepareVO.setTid((String)session.getAttribute("tid"));
		
		session.removeAttribute("partner_order_id");
		session.removeAttribute("partner_user_id");
		session.removeAttribute("tid");
		
		KakaoPayApproveVO approveVO = payService.approve(prepareVO);
		
		//?????? ?????? ?????? ????????? ?????? ?????? ?????? ?????????????????? ??????????????? ??????
		return "redirect:historyDetail?paymentNo="+prepareVO.getPartner_order_id();
	}

	@Autowired
	private PaymentDao paymentDao;
	
	@GetMapping("/history")
	public String history(HttpSession session, Model model) {
		int memberNo = (int)session.getAttribute("memberNo");
		model.addAttribute("list", paymentDao.list(memberNo));
		return "shop/history";
	}
	
	@GetMapping("/historyDetail")
	public String historyDetail(@RequestParam int paymentNo, Model model) throws URISyntaxException {
		//?????? ?????? ??????
		PaymentDto paymentDto = paymentDao.get(paymentNo);
		model.addAttribute("paymentDto", paymentDto);
		
		//????????? ?????? ??????
		KakaoPaySearchVO searchVO = payService.search(paymentDto.getPaymentTid());
		model.addAttribute("searchVO", searchVO);
		
		return "shop/historyDetail";
	}
	
	@GetMapping("/refund")
	public String refund(
			//paymentNo, cancel_amount??? ????????????
			@ModelAttribute KakaoPayCancelPrepareVO prepareVO
			) throws URISyntaxException {
		payService.cancel(prepareVO);
		return "redirect:historyDetail?paymentNo="+prepareVO.getPaymentNo();
	}
	
	//?????? ?????? ?????? ??????
		@PostMapping("/buy2")
		public String buy2(@RequestParam int[] no, Model model) {
			log.debug("no = {}", Arrays.toString(no));
			model.addAttribute("list", productDao.list(no));
			return "shop/buy2";
		}
		
//		[1] chunk?????? ???????????? ???????????? ?????? ?????????(????????? ????????? ?????? ?????? ?????????)
//		public String confirm(@RequestParam ChunkVO[] chunk) {
//		@PostMapping("/confirm2")
//			log.debug("chunk = {}", Arrays.toString(chunk));
//			return "redirect:https://www.naver.com";
//		}
		
//		[2] TestVO ?????? ?????? chunk ????????? chunk ?????? ???????????? ??? ?????? ?????????
//		= @ModelAttribute??? ?????? ????????? ??? ??????????????? ????????? ????????? ??????
		@PostMapping("/confirm2")
		public String confirm(@ModelAttribute TestVO testVO,
				HttpSession session) throws URISyntaxException {
			List<ChunkVO> chunk = testVO.getChunk();
			log.debug("chunk = {}", chunk);
//			if(chunk == null || chunk.isEmpty()) return "redirect:???????????????";
			
//			chunk ?????? ?????? ??????????????? ???????????? ?????? ????????? ?????? + DB?????? ??????
//			= ????????? ?????? ????????? ???????????? ??????(??? ??????)
			int memberNo = (int)session.getAttribute("memberNo");
			ProductDto firstProductDto = productDao.get(chunk.get(0).getNo());
			String itemName = firstProductDto.getName() + " ??? " + (chunk.size()-1) + "???";
			
			int totalAmount = 0;
			int[] no = new int[chunk.size()];
			for(int i=0; i < no.length; i++) {
				no[i] = chunk.get(i).getNo();
			}
			List<ProductDto> choiceList = productDao.list(no);
			
			for(int i=0; i < chunk.size(); i++) {
				//????????? += ???????????? * ????????????;
				totalAmount += choiceList.get(i).getPrice() * chunk.get(i).getQuantity();
			}
			
			KakaoPayReadyPrepareVO prepareVO = 
						KakaoPayReadyPrepareVO.builder()
											.item_name(itemName)
											.quantity(1)
											.partner_user_id(String.valueOf(memberNo))
											.total_amount(totalAmount)
											.no(no[0])
											.chunk(chunk)
											.product(choiceList)
										.build();
			KakaoPayReadyVO readyVO = payService.ready2(prepareVO);
			
//			?????? ????????? ??????(success?????? ???????????? ??????)
			session.setAttribute("partner_order_id", readyVO.getPartner_order_id());
			session.setAttribute("partner_user_id", readyVO.getPartner_user_id());
			session.setAttribute("tid", readyVO.getTid());
			
			return "redirect:"+readyVO.getNext_redirect_pc_url();
		}
		
	}











