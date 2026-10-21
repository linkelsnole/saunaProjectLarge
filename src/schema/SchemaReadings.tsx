export function AirflowCard({x,y,target,actual}: {x:number;y:number;target:string;actual:string}) {
 const number=(text:string)=>text.split(' ')[0];
 return <div aria-label="Расход воздуха: цель и факт" style={{position:'absolute',left:x,top:y+10,width:76,padding:'5px 7px',boxSizing:'border-box',background:'#fff',border:'1px solid #e2e8f0',borderRadius:8,boxShadow:'0 2px 5px #0f172a08',fontFamily:'Inter, Arial, sans-serif',fontVariantNumeric:'tabular-nums',pointerEvents:'none'}}>
 {[['Цель',target,'#059669'],['Факт',actual,'#0f172a']].map(([label,value,color])=><div key={label} title={label} style={{display:'flex',alignItems:'baseline',justifyContent:'space-between',gap:3,color,lineHeight:'22px'}}><strong style={{fontSize:13,fontWeight:500}}>{number(value)}</strong><span style={{fontSize:8,color:'#64748b'}}>м³/ч</span></div>)}</div>;
}
