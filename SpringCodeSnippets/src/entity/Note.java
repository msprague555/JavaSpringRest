package entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(catalog="frontdoor", schema="dbo", name="note")
public class Note {

	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Serno", nullable=false) 	
	private Integer serno;
	
	@Column(name="ReqSerno")
	private Integer reqSerno;
	
	@Column(name="Attuid")
	private String attuid;
	
	@Column(name="Note")
	private String note;
	
	@Column(name="InsDate", columnDefinition="date" )
	@Temporal(TemporalType.DATE)
	@JsonSerialize(as = Date.class)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")	
	private Date insDate;

	@Column(name="Type", columnDefinition="tinyint" )
	private Integer type;
	
	@Column(name="GbrFlag", columnDefinition="bit" )
	private Boolean gbrFlag;
	
	public Note() {
		
	}

	public Note(Integer serno, Integer reqSerno, String attuid, String note, Date insDate, Integer type,
			Boolean gbrFlag) {
		super();
		this.serno = serno;
		this.reqSerno = reqSerno;
		this.attuid = attuid;
		this.note = note;
		this.insDate = insDate;
		this.type = type;
		this.gbrFlag = gbrFlag;
	}
	
	public Note(Integer reqSerno, String attuid, String note, Date insDate, Integer type,
			Boolean gbrFlag) {
		super();
		this.reqSerno = reqSerno;
		this.attuid = attuid;
		this.note = note;
		this.insDate = insDate;
		this.type = type;
		this.gbrFlag = gbrFlag;
	}

	public Integer getSerno() {
		return serno;
	}

	public void setSerno(Integer serno) {
		this.serno = serno;
	}

	public Integer getReqSerno() {
		return reqSerno;
	}

	public void setReqSerno(Integer reqSerno) {
		this.reqSerno = reqSerno;
	}

	public String getAttuid() {
		return attuid;
	}

	public void setAttuid(String attuid) {
		this.attuid = attuid;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Date getInsDate() {
		return insDate;
	}

	public void setInsDate(Date insDate) {
		this.insDate = insDate;
	}
	
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Boolean getGbrFlag() {
		return gbrFlag;
	}

	public void setGbrFlag(Boolean gbrFlag) {
		this.gbrFlag = gbrFlag;
	}
	
	
}
