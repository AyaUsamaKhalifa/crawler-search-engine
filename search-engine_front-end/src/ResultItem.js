import React from 'react';
import './ResultItem.css';

function ResultItem(props) {
  const [paragraph, setParagraph] = React.useState("");
  React.useEffect(() => {
    const modifiedText = props.searchedWords.reduce((Paragraph, currWord) => {
      return Paragraph.replace(new RegExp(currWord, "gi"), `<b>${currWord}</b>`);
    }, props.paragraph);
    setParagraph(modifiedText);
  }
    ,
    [props.paragraph, props.searchedWords])
  return (
    <div className='result-item'>
      <h2>{props.title}</h2>
      <a href={props.link}>{props.link}</a>
      <p dangerouslySetInnerHTML={{ __html: paragraph }} />
    </div>
  );
}

export default ResultItem;