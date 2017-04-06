package tp.utn.demo.domain;

import java.sql.Date;

import tp.utn.ann.Column;
import tp.utn.ann.Id;
import tp.utn.ann.Table;

@Table(name="emp")
public class Emp
{
	@Id(strategy=Id.IDENTITY)
	@Column(name="empno")
	private int empno;
	
	@Column(name="ename")
	private String ename;
	
	@Column(name="hiredate")
	private Date hiredate;

	@Column(name="deptno")
	private Dept dept;

	public int getEmpno()
	{
		return empno;
	}

	public void setEmpno(int empno)
	{
		this.empno=empno;
	}

	public String getEname()
	{
		return ename;
	}

	public void setEname(String ename)
	{
		this.ename=ename;
	}

	public Date getHiredate()
	{
		return hiredate;
	}

	public void setHiredate(Date hiredate)
	{
		this.hiredate=hiredate;
	}

	public Dept getDept()
	{
		return dept;
	}

	public void setDept(Dept dept)
	{
		this.dept=dept;
	}
	
	@Override
	public boolean equals(Object other)
	{
		Emp o=(Emp)other;
		System.out.println("yo = "+this);
		System.out.println(" o = "+o);
		
		if( empno==o.getEmpno() )
		{
			System.out.println("aaaaaaaaaaaaaaaaaaaaaa");
		}
		
		
		return (empno==o.getEmpno());//&&o.ename.equals(ename)&&o.hiredate.equals(hiredate);
	}

	@Override
	public String toString()
	{
		return "Emp [empno="+empno+", ename="+ename+", hiredate="+hiredate+", "+dept+"]";
	}		
}
