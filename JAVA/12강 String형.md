1\. 자바에서 문자열을 저장하기 위해 사용하는 대표적인 참조 자료형의 이름을 작성하시오.

String



2\. String 객체를 생성하는 두 가지 방식인 '리터럴 선언'과 'new 연산자 사용'의 차이점을 메모리 할당 관점에서 서술하시오.

|방식|저장 위치|특징|
|-|-|-|
|String str = "안녕"|Heap > String Pool|같은 값이면 기존 객체 재사용|
|String str = new String("안녕")|Heap (일반 영역)|항상 새 객체 생성, Pool 미사용|



3 String 객체를 생성하는 두 가지 방식인 '리터럴 선언'을 String str2=" 안녕"; String str3="안녕";에서 str2와 str3를 메모리 공유 입장에서 설명하시오.

두 값이 다르므로 String Pool에 각각 별도 객체 생성됨

str2 == str3 → false

반면, 값이 같으면 공유된다



4\. 두 문자열의 '값' 자체가 동일한지 비교할 때 사용하는 메서드 이름을 작성하시오.

s.equals(other)



5\. 문자열의 길이를 반환하는 메서드와 호출 형식을 작성하시오.

s.length()



6\. 특정 인덱스에 위치한 문자 하나를 추출할 때 사용하는 메서드를 작성하시오.

s.charAt(2)



7\. 문자열 내에서 특정 문자나 부분 문자열이 처음으로 등장하는 위치(인덱스)를 찾는 메서드를 작성하시오.

s.indexOf("Java")



8\. 문자열의 앞뒤 공백을 제거하여 반환하는 메서드 이름을 작성하시오.

s.trim()



9\. 기존 문자열에서 특정 부분을 잘라내어 새로운 문자열을 얻을 때 사용하는 메서드를 작성하시오.

s.substring(2, 7)



10\. 문자열을 특정 구분자(Delimiter)를 기준으로 나누어 배열로 반환하는 메서드를 작성하시오.

s.split(", ")



\# substring(start, end) → end 인덱스는 포함 안 됨

\# indexOf() → 못 찾으면 -1 반환 (0이 아님)



자바 문자열 

1\. str2.equals(str3)



2\. substring



3\. def clean\_user\_data(input\_str):

&#x20;   """

&#x20;   1. 앞뒤 공백 제거 (trim)

&#x20;   2. 중간 공백을 언더바(\_)로 치환 (replace)

&#x20;   """

&#x20;   # 1. strip()으로 앞뒤 공백 제거

&#x20;   stripped\_str = input\_str.strip()

&#x20;   

&#x20;   # 2. replace()로 공백(" ")을 언더바("\_")로 변경

&#x20;   cleaned\_str = stripped\_str.replace(" ", "\_")

&#x20;   

&#x20;   return cleaned\_str



\# --- 테스트 케이스 ---

raw\_data\_list = \[

&#x20;   "  user123  ",

&#x20;   "john doe ",

&#x20;   "  data  processing  ",

&#x20;   "no\_spaces",

&#x20;   "  leading and trailing  "

]



print(f"{'원본 데이터':<25} | {'정제된 데이터'}")

print("-" \* 40)

for data in raw\_data\_list:

&#x20;   print(f"'{data}':<25 | '{clean\_user\_data(data)}'")



4\. 

def parse\_email(email):

&#x20;   # 1. @ 기호 포함 여부 확인 (contains 기능)

&#x20;   if "@" in email:

&#x20;       # 2. @ 기호를 기준으로 분리 (split)

&#x20;       # split('@')은 리스트를 반환함: \['아이디', '도메인']

&#x20;       parts = email.split("@")

&#x20;       

&#x20;       username = parts\[0]

&#x20;       domain = parts\[1]

&#x20;       

&#x20;       print(f"이메일: {email}")

&#x20;       print(f"아이디: {username}")

&#x20;       print(f"도메인: {domain}")

&#x20;       print("-" \* 20)

&#x20;   else:

&#x20;       print(f"이메일: {email}")

&#x20;       print("오류: 올바른 이메일 형식이 아닙니다. (@ 기호가 없음)")

&#x20;       print("-" \* 20)



\# 테스트

email1 = "user01@example.com"

email2 = "invalid-email.com"

email3 = "test.name@company.co.kr"



parse\_email(email1)

parse\_email(email2)

parse\_email(email3)





