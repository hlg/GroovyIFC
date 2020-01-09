import java.util.UUID

charset = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$'

def weakRandom_1(){
  new Random().with{
     charset[ nextInt() & 3 ] + (1..21).collect{ charset[ nextInt() & 63 ] }.join()  
  }
}

def weakRandom_2(){
  byte[] bytes = new byte[22]
  new Random().nextBytes( bytes )
  charset[ bytes[0]&3 ] + bytes[1..21].collect{ charset[ it&63 ] }.join()
}


def strongRandom(){
  def uuid = new BigInteger(UUID.randomUUID().toString().replace('-',''),16)
  (21..0).collect{ i -> charset[uuid.shiftRight(i*6) & 63] }.join('')
}

10.times{ println strongRandom() }

