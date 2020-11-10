# Hillo!
### 개요
Hillo!는 카카오 계정과 연동되어, 친구와 만날 시간을 손쉽게 정하게 해주는 앱입니다.

### 기능

**0. 카카오 계정 로그인**

처음 앱 시작 시 카카오 로그인 API를 활용해 카카오 계정으로 로그인하게 됩니다.

![Screenshot_20200813-132920_Hillo!](https://user-images.githubusercontent.com/37796241/90999589-049de080-e602-11ea-9db0-c0c5b69e9df8.jpg)

**1. 시간표 입력**

자신의 시간표를 미리 입력하여 만날 시간을 쉽게 정할 수 있습니다.

![Screenshot_20200813-133015_Hillo!](https://user-images.githubusercontent.com/37796241/90999196-f4393600-e600-11ea-8d63-ba725c2d0ed1.jpg)


**2. 추천 만남**

시간표가 서버에 업로드 되었고 카카오 계정으로 로그인 되었다면, 서버에서 친구와 함께 비는 시간을 추출해 추천 만남 리스트를 제공합니다.

![Screenshot_20200813-132852_Hillo!](https://user-images.githubusercontent.com/37796241/90999530-d15b5180-e601-11ea-9f8a-d3fc12cdd562.jpg)

추천 만남에서 '만나기' 버튼을 누른다면, 카카오톡 메시지 API를 사용해 해당 친구에게 만남을 요청하는 카카오톡 메시지를 보냅니다.


**3. 만남 시작**

친구들과 만날 시간을 손쉽게 정할 수 있습니다. 

![Screenshot_20200813-133050_Hillo!](https://user-images.githubusercontent.com/37796241/91000232-a83bc080-e603-11ea-9953-2619e5a262e2.jpg)

시작 버튼을 누르면, 카카오 링크 API를 사용해 원하는 채팅방에 만남 시간을 정할 세션 링크를 보냅니다.

![Screenshot_20200813-175332_KakaoTalk](https://user-images.githubusercontent.com/37796241/98649880-7bce4f80-237b-11eb-8693-2661b695e6a0.jpg)

해당 세션에 모든 인원이 참가하였다면, 방장이 세션 출입을 마감하고 각 인원이 원하는 시간에 투표하게 됩니다. 
