def charset = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$'

new Random().with{
   println charset[ nextInt() & 3 ] + (1..21).collect{ charset[ nextInt() & 63 ] }.join()  
}

byte[] bytes = new byte[22]
new Random().nextBytes( bytes )
println charset[ bytes[0]&3 ] + bytes[1..21].collect{ charset[ it&63 ] }.join()



