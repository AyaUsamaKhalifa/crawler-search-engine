import React from "react";
import "./ResultItem.css";

function ResultItem(props) {
  const [paragraph, setParagraph] = React.useState("");
  React.useEffect(() => {
    const modifiedParagraph = props.paragraph
      .split(/\b/)
      .map((currWord) => {
        let temp = currWord.toLowerCase();
        if (
          props.searchedWords.some((currentWord) => temp.includes(currentWord))
        ) {
          return `<b>${currWord}</b>`;
        } else {
          return currWord;
        }
      })
      .join("");
    setParagraph(modifiedParagraph);
  }, [props.paragraph, props.searchedWords]);
  return (
    <div className="result-item">
      <h2>{props.title}</h2>
      <a href={props.link}>{props.link}</a>
      <p dangerouslySetInnerHTML={{ __html: paragraph }} />
    </div>
  );
}

export default ResultItem;
