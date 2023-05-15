import React from "react";
import "./ResultsPage.css";
import Image from "./images/Search2.png";
import SearchArea from "./SearchArea";
import ResultsArea from "./ResultsArea";
import { Link } from "react-router-dom";
import { useParams } from "react-router-dom";
import axios from "axios";

function ResultsPage() {
  const [searchResult, setSearchResults] = React.useState([]);
  let { SearchResult } = useParams();
  // React.useEffect(() => {
  //   (async () => {
  //     await axios.get(`http://localhost:8081/PopularSearches/${encodeURIComponent(SearchResult)}`)
  //       .then((response) => {
  //         setSearchResults(response.data);
  //         console.log(response.data);
  //       })
  //       .catch((error) => {
  //         console.error(error);
  //       });
  //   })();
  // }, [SearchResult]);
  return (
    <div className="results-page">
      <div className="search-section">
        <Link to="/" className="link">
          <img className="image-features" src={Image} alt="Search" />
        </Link>
          <SearchArea SearchResults={searchResult} />
      </div>
      <ResultsArea />
    </div>
  );
}

export default ResultsPage;
