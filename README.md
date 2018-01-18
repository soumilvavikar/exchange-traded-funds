# exchange-traded-funds

Info APIs
Get My Info - http://localhost:10007/api/info/me
Get My Peers - http://localhost:10007/api/info/peers

Sponsorer APIs -
issueETF - http://localhost:10010/api/etf/issue?etfName=GlobalFinancialsETF&quantity=10&etfCode=etf123
getEtf - http://localhost:10010/api/etf/get
iou - http://localhost:10010/api/etf/iou?etfName=GlobalFinancialsETF1&quantity=10&etfCode=etf123&party=AuthorizedParticipant
settle-iou - http://localhost:10010/api/etf/settle-iou?etfCode=etf44&etfHash=a3dd180d-7d0b-4052-a8b4-8457025fab4e

Authorized Participant APIs -
issueBasket - http://localhost:10007/api/security-basket/issue?basketIpfsHash=soumil12334&party=AuthorizedParticipant
getBasket - http://localhost:10007/api/security-basket/get
createEtf - http://localhost:10007/api/etf-request/create?basketIpfsHash=soumil12334&sponserer=Sponsorer&etfCode=etf123
getEtf - http://localhost:10007/api/etf-request/get

Notary APIs -
get-data - http://localhost:10004/api/notary/get-data