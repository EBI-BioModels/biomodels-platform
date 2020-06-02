#include <stdio.h>
#include <math.h>

void sum_from_1_to_n(int n){
  int i,s;
  s=0;
  printf("Nhap n: ");
  scanf("%d",&n);
  for (i=1; i<=n; i++)
    s += i;
  printf("\nTong la: %d\n",s);
}

void giai_ptb2(){
  float x1,x2,x,delta;
  float a,b,c;
  printf("Nhap a, b, c lan luot la: ");
  scanf("%f %f %f",&a, &b, &c);
  delta = b*b-4*a*c;
  if (delta<0)
    printf("PT Vo nghiem");
  else if(delta==0)
    {
      x = -b/2*a;
      printf("PT co nghiem kep: x1 = x2 = %2.2f",x);
    }
  else {
    x1=(-b+sqrt(delta))/(2*a);
    x2=(-b-sqrt(delta))/(2*a);
    printf("Pt co 2 nghiem phan biet: \n x1 = %2.4f \n x2 = %2.4f",x1,x2);
  }
}

int main(){
  sum_from_1_to_n(100);
  giai_ptb2();
  printf("\nBye\n");
}
